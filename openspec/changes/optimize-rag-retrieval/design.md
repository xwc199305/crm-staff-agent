# RAG Retrieval Capability Optimization - Detailed Design

## Architecture Design

### Overall Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        User Request                             │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    IntentRecognizer                             │
│                    (Intent Recognition)                         │
└─────────────────────────────┬───────────────────────────────────┘
                              │ IntentType
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    IntentHandler                                │
│                    (Intent Handler)                             │
└─────────────────────────────┬───────────────────────────────────┘
                              │ query
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    DifyKnowledgeBaseService                     │
│                    (Knowledge Base Retrieval)                   │
└─────────────────────────────┬───────────────────────────────────┘
                              │ List<Record>
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    RagService                                   │
│                    (RAG Generation Service)                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 1. Assemble Prompt (template + JSON context + query)    │   │
│  │ 2. Call LLM to generate response                        │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────┬───────────────────────────────────┘
                              │ String (Final Response)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Return to User                           │
└─────────────────────────────────────────────────────────────────┘
```

### Core Component Responsibilities

| Component | Responsibility |
|------|------|
| `DifyKnowledgeBaseService` | Retrieve knowledge base, return raw records list |
| `RagService` | Assemble Prompt, call LLM, return generated result |
| `IntentHandler` | Call RAG service based on intent type |

## Detailed Design

### 1. Prompt Template Design

Configure RAG Prompt template in `application.properties`:

```properties
rag.prompt-template=You are a professional customer service assistant. Please answer the user's question based on the provided knowledge base content.

Knowledge Base Content:
{{context}}

User Question: {{query}}

Requirements:
1. Only answer based on provided knowledge base content, do not fabricate information
2. If no relevant information in knowledge base, clearly state "No relevant information"
3. Answers should be clear, concise, and organized
4. Can use Markdown format appropriately to improve readability
```

**Placeholder Description**:
- `{{context}}` - Knowledge base retrieval results (JSON format)
- `{{query}}` - Original user question

### 2. Record to JSON Format

Assemble Dify returned records into the following JSON format:

```json
[
  {
    "content": "Matched text content",
    "score": 0.85,
    "document_name": "Document name",
    "keywords": ["keyword1", "keyword2"]
  },
  {
    "content": "Another matched content",
    "score": 0.72,
    "document_name": "Document name 2",
    "keywords": ["keyword3"]
  }
]
```

### 3. DifyKnowledgeBaseService Extension

**Interface Extension**:

```java
public interface DifyKnowledgeBaseService {
    Optional<String> query(String query);
    List<DifyResponse.Record> retrieveRecords(String query);
    boolean isEnabled();
}
```

**Implementation Notes**:
- `retrieveRecords()` method directly returns Dify API's records list
- Keep original `query()` method compatible with old code

### 4. RagService Design

**Interface Definition**:

```java
public interface RagService {
    String generate(String query, List<DifyResponse.Record> records);
    String generateWithQuery(String query);
}
```

**Implementation Logic**:

```
1. Validate if records is empty
2. Convert records to JSON format string
3. Replace placeholders in Prompt template
4. Call LLM to generate response
5. Return generated result
```

### 5. Intent Handler Updates

Update intent handlers to use new RAG flow:

```java
@Override
public String handle(String query) {
    // 1. Retrieve knowledge base
    List<DifyResponse.Record> records = knowledgeBaseService.retrieveRecords(query);
    
    if (records != null && !records.isEmpty()) {
        // 2. Use RAG to generate response
        String ragResult = ragService.generate(query, records);
        if (ragResult != null && !ragResult.isEmpty()) {
            return ragResult;
        }
    }
    
    // 3. Fallback to local knowledge base
    return fallbackAnswer(query);
}
```

## Data Structure Design

### RAG Context Structure

```java
public class RagContext {
    private List<RagRecord> records;
    
    public static class RagRecord {
        private String content;
        private Double score;
        private String documentName;
        private List<String> keywords;
    }
}
```

## Configuration Items Design

| Item | Type | Default | Description |
|--------|------|--------|------|
| `rag.prompt-template` | String | (see above) | RAG Prompt template |
| `rag.max-records` | Integer | 3 | Maximum returned records |
| `rag.max-content-length` | Integer | 2000 | Maximum content length per record |

## Error Handling

| Scenario | Handling |
|------|----------|
| Dify not configured or disabled | Use local knowledge base fallback |
| Dify retrieval failed | Use local knowledge base fallback |
| LLM call failed | Return raw knowledge base content |
| Context too long | Truncate content or reduce record count |

## Performance Optimization

1. **Record Count Limit**: Limit returned records via `rag.max-records`
2. **Content Truncation**: Limit single record length via `rag.max-content-length`
3. **Caching Mechanism**: Cache retrieval results for identical queries (optional)
4. **Async Processing**: Support async calls (optional)