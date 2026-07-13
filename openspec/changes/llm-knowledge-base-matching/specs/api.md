# API Specification

## Internal Interfaces

### KnowledgeBaseMatcher Interface

#### match Method

**Function**: Match the most suitable knowledge base based on intent type, user query, and knowledge base list

**Parameters**:

| Parameter | Type | Description |
|-----------|------|-------------|
| intentType | IntentType | Intent type enum |
| query | String | User query content |
| knowledgeBaseList | List\<KnowledgeBaseInfo\> | Knowledge base list |

**Return Value**:

| Return Type | Description |
|-------------|-------------|
| String | Matched knowledge base ID, return "DEFAULT" if no suitable match |

**Example**:

```java
KnowledgeBaseInfo kb1 = new KnowledgeBaseInfo();
kb1.setId("dataset-123");
kb1.setName("Product Usage Guide");
kb1.setDescription("Contains product usage methods, feature introductions, etc.");

KnowledgeBaseInfo kb2 = new KnowledgeBaseInfo();
kb2.setId("dataset-456");
kb2.setName("Aftersales Manual");
kb2.setDescription("Contains aftersales processes, return/refund policies, etc.");

List<KnowledgeBaseInfo> kbList = Arrays.asList(kb1, kb2);

String datasetId = matcher.match(IntentType.PRODUCT_CONSULTATION, "How to use product features", kbList);
// Returns: "dataset-123"
```

### LLMKnowledgeBaseMatcher Implementation

#### Prompt Template

```
You are a knowledge base classification expert. Please select the most appropriate knowledge base from the list below for retrieval based on the user's intent and query content.

Intent Type: {intentType}
Intent Description: {intentDescription}
User Query: {query}

Knowledge Base List:
{knowledgeBaseList}

Please return the most suitable knowledge base ID. If no suitable knowledge base, return "DEFAULT".

Return Format:
Knowledge Base ID: {datasetId}
```

#### Parameter Replacement Rules

| Placeholder | Replacement Content | Source |
|-------------|---------------------|--------|
| {intentType} | Intent type name | IntentType.name() |
| {intentDescription} | Intent description | IntentType.getDescription() |
| {query} | User query content | User input |
| {knowledgeBaseList} | Knowledge base list string | Formatted knowledge base info |

#### Knowledge Base List Formatting

Each knowledge base info format:
```
- ID: {id} | Name: {name} | Description: {description} | Tags: {tags}
```

#### LLM Response Parsing

Extract knowledge base ID from LLM response:
1. Find "Knowledge Base ID:" keyword
2. Extract content after it, remove whitespace
3. If "DEFAULT" is returned, no suitable knowledge base matched

### DifyKnowledgeBaseServiceImpl Interface Extension

#### retrieveRecordsByIntent Method

**Function**: Dynamically select knowledge base for retrieval based on intent type and user query

**Parameters**:

| Parameter | Type | Description |
|-----------|------|-------------|
| query | String | User query content |
| intentType | IntentType | Intent type enum |

**Return Value**:

| Return Type | Description |
|-------------|-------------|
| List\<DifyResponse.Record\> | Retrieved records list, returns empty list on failure or no results |

**Matching Chain**:

1. Configuration mapping (highest priority)
2. LLM matching (cached)
3. LLM matching (real-time)
4. Keyword matching
5. Default dataset (fallback)

#### refreshKnowledgeBaseList Method

**Function**: Refresh knowledge base list and invalidate cache

**Parameters**: None

**Return Value**: None

**Side Effects**:
- Update cached knowledge base list
- Clear LLM matching result cache

#### getKnowledgeBaseList Method

**Function**: Get current cached knowledge base list

**Parameters**: None

**Return Value**:

| Return Type | Description |
|-------------|-------------|
| List\<KnowledgeBaseInfo\> | Knowledge base list |

#### isEnabled Method

**Function**: Determine if knowledge base service is enabled

**Parameters**: None

**Return Value**:

| Return Type | Description |
|-------------|-------------|
| boolean | true means enabled, false means disabled |

## Configuration Interfaces

### application.properties Configuration Items

| Configuration Item | Type | Default Value | Description |
|--------------------|------|---------------|-------------|
| kb.matcher.llm.enabled | boolean | true | Whether to enable LLM matching |
| kb.matcher.llm.prompt-template | string | See prompt template | LLM matching prompt template |
| kb.matcher.llm.timeout-seconds | int | 5 | LLM call timeout (seconds) |
| kb.matcher.llm.model-name | string | qwen-max | LLM model name |

### Prompt Template Configuration Example

```properties
kb.matcher.llm.prompt-template=You are a knowledge base classification expert. Please select the most appropriate knowledge base from the list below for retrieval based on the user's intent and query content.\n\nIntent Type: {intentType}\nIntent Description: {intentDescription}\nUser Query: {query}\n\nKnowledge Base List:\n{knowledgeBaseList}\n\nPlease return the most suitable knowledge base ID. If no suitable knowledge base, return \"DEFAULT\".\n\nReturn Format:\nKnowledge Base ID: {datasetId}
```

## Error Handling Interfaces

### Exception Types

| Exception | Scenario | Handling Method |
|-----------|----------|-----------------|
| LLM call timeout | LLM response time exceeds configured timeout | Degrade to keyword matching or default dataset |
| LLM call failed | LLM service unavailable or returns error | Degrade to keyword matching or default dataset |
| Response format error | LLM return format doesn't match expectation | Degrade on parse failure |
| Knowledge base list is empty | Failed to get knowledge base list or empty | Use default dataset directly |

### Degradation Chain

```
LLM Matching Failed → Keyword Matching → Default Dataset
```

## Cache Interfaces

### LLM Matching Result Cache

**Cache Key**: IntentType enum value

**Cache Value**: Knowledge base ID string

**Cache Strategy**:

| Strategy | Description |
|----------|-------------|
| Write Timing | After LLM matching succeeds |
| Read Timing | Check cache first when matching intent |
| Invalidation Timing | When knowledge base list is refreshed |
| TTL | Same as knowledge base list refresh interval |

**Cache Management Methods**:

| Method | Function |
|--------|----------|
| getCachedMatch | Get cached matching result |
| cacheMatch | Cache matching result |
| invalidateCache | Clear all cache |