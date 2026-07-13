# LLM-based Knowledge Base Intelligent Matching - Detailed Design

## Architecture Design

### Overall Architecture

```
User Query → Intent Recognition → Knowledge Base Matching → Knowledge Base Retrieval → RAG Generation → Response
                        ↓
              ┌─────────┴─────────┐
              │   KnowledgeBase   │
              │     Matcher       │
              └─────────┬─────────┘
                        ↓
              ┌───────────────────┐
              │  LLM Matcher      │
              │  Keyword Matcher  │
              │  Config Mapping   │
              └───────────────────┘
```

### Component Responsibilities

| Component | Responsibility |
|-----------|---------------|
| KnowledgeBaseMatcher | Knowledge base matcher interface, defines matching method |
| LLMKnowledgeBaseMatcher | LLM-based knowledge base matcher implementation |
| DifyKnowledgeBaseServiceImpl | Integrates all matching logic, manages cache and matching chain |

## Interface Design

### KnowledgeBaseMatcher Interface

```java
public interface KnowledgeBaseMatcher {
    String match(IntentType intentType, String query, List<KnowledgeBaseInfo> knowledgeBaseList);
}
```

### LLMKnowledgeBaseMatcher Implementation

```java
public class LLMKnowledgeBaseMatcher implements KnowledgeBaseMatcher {
    String match(IntentType intentType, String query, List<KnowledgeBaseInfo> knowledgeBaseList);
}
```

### DifyKnowledgeBaseService Interface Extension

```java
public interface DifyKnowledgeBaseService {
    // Existing methods remain unchanged
    Optional<String> query(String query);
    List<DifyResponse.Record> retrieveRecords(String query);
    List<DifyResponse.Record> retrieveRecordsByIntent(String query, IntentType intentType);
    List<KnowledgeBaseInfo> getKnowledgeBaseList();
    void refreshKnowledgeBaseList();
    boolean isEnabled();
}
```

## Data Structures

### KnowledgeBaseInfo

Already exists, no modification needed:
- id: String - Knowledge base ID
- name: String - Knowledge base name
- description: String - Knowledge base description
- tags: List<String> - Tag list
- created_at: String - Creation time
- updated_at: String - Update time

## Implementation Details

### LLM Prompt Template

Configured in `application.properties`:

```properties
kb.matcher.llm.prompt-template=You are a knowledge base classification expert. Please select the most appropriate knowledge base from the list below for retrieval based on the user's intent and query content.

Intent Type: {intentType}
Intent Description: {intentDescription}
User Query: {query}

Knowledge Base List:
{knowledgeBaseList}

Please return the most appropriate knowledge base ID. If no suitable knowledge base is found, return "DEFAULT".

Return Format:
Knowledge Base ID: {datasetId}
```

### LLM Call Implementation

Use `DashScopeChatModel` directly without `ReActAgent`:

```java
private String callLlmForMatching(String prompt) {
    DashScopeChatModel model = DashScopeChatModel.builder()
            .apiKey(apiKey)
            .modelName("qwen-max")
            .timeout(Duration.ofSeconds(5))
            .build();
    
    Msg msg = Msg.builder()
            .textContent(prompt)
            .build();
    
    Mono<Msg> responseMono = model.call(msg);
    Msg response = responseMono.block(Duration.ofSeconds(5));
    
    return response != null ? response.getTextContent() : "";
}
```

### Cache Mechanism

Use `ConcurrentHashMap` as cache:

```java
private Map<IntentType, String> llmMatchCache = new ConcurrentHashMap<>();

private void invalidateCache() {
    llmMatchCache.clear();
}

private String getCachedMatch(IntentType intentType) {
    return llmMatchCache.get(intentType);
}

private void cacheMatch(IntentType intentType, String datasetId) {
    llmMatchCache.put(intentType, datasetId);
}
```

### Matching Chain Implementation

```java
private String findDatasetIdForIntent(IntentType intentType, String query) {
    // 1. Config mapping (highest priority)
    String datasetId = intentDatasetMap.get(intentType);
    if (datasetId != null && !datasetId.isEmpty()) {
        return datasetId;
    }
    
    // 2. LLM matching (cached)
    datasetId = getCachedMatch(intentType);
    if (datasetId != null && !datasetId.isEmpty()) {
        if ("DEFAULT".equals(datasetId)) {
            // Use default dataset
        } else {
            return datasetId;
        }
    }
    
    // 3. LLM matching (real-time)
    datasetId = llmKnowledgeBaseMatcher.match(intentType, query, cachedKnowledgeBaseList);
    if (datasetId != null && !datasetId.isEmpty()) {
        cacheMatch(intentType, datasetId);
        if (!"DEFAULT".equals(datasetId)) {
            return datasetId;
        }
    }
    
    // 4. Keyword matching
    datasetId = autoMatchKnowledgeBase(intentType);
    if (datasetId != null && !datasetId.isEmpty()) {
        return datasetId;
    }
    
    // 5. Default dataset (fallback)
    return difyClient.getDefaultDatasetId();
}
```

## Configuration Items

| Configuration Item | Description | Default Value |
|--------------------|-------------|---------------|
| kb.matcher.llm.enabled | Whether to enable LLM matching | true |
| kb.matcher.llm.prompt-template | LLM matching prompt template | See above |
| kb.matcher.llm.timeout-seconds | LLM call timeout (seconds) | 5 |
| kb.matcher.llm.model-name | LLM model name | qwen-max |

## Error Handling

1. **LLM call failure**: Degrade to keyword matching or default dataset
2. **LLM returns invalid format**: Degrade on parsing failure
3. **Empty knowledge base list**: Use default dataset directly
4. **Timeout handling**: Configurable timeout with degradation

## Performance Optimization

1. **Result caching**: Cache intent→knowledge base mapping to avoid repeated LLM calls
2. **Knowledge base list caching**: Cache knowledge base list to reduce API calls
3. **Asynchronous refresh**: Knowledge base list refresh doesn't block user requests
4. **Lightweight LLM call**: Use model directly without ReActAgent

## Test Points

1. **LLM matching accuracy**: Verify different intent types correctly match corresponding knowledge bases
2. **Cache mechanism**: Verify cache works, same intent doesn't call LLM repeatedly
3. **Cache invalidation**: Verify cache invalidates after knowledge base list refresh
4. **Degradation mechanism**: Verify degradation to keyword matching or default dataset when LLM fails
5. **Timeout handling**: Verify timeout configuration works