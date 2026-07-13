# Data Specification

## Data Structure

### KnowledgeBaseInfo

**Purpose**: Represents knowledge base information

**Fields**:

| Field Name | Type | Description | Constraint |
|------------|------|-------------|------------|
| id | String | Knowledge base unique identifier | Not null |
| name | String | Knowledge base name | Not null, max length 255 |
| description | String | Knowledge base description | Nullable, max length 2000 |
| tags | List\<String\> | Tag list | Nullable |
| created_at | String | Creation time | Nullable, ISO 8601 format |
| updated_at | String | Update time | Nullable, ISO 8601 format |

**Example**:

```json
{
  "id": "5792187f-02e8-4b5c-a674-7efb72e8174f",
  "name": "User Guide",
  "description": "Product usage guide, including feature introduction, operation instructions, etc.",
  "tags": ["product", "usage", "guide"],
  "created_at": "2024-01-15T10:30:00Z",
  "updated_at": "2024-01-20T14:45:00Z"
}
```

### IntentType

**Purpose**: Represents intent type enum

**Enum Values**:

| Enum Value | Description | Matching Keywords |
|------------|-------------|-------------------|
| PRODUCT_CONSULTATION | Product Usage Consultation | product, usage, function, operation, guide |
| WARRANTY_POLICY | Warranty Policy Consultation | warranty, guarantee |
| AFTERSALES_PROCESS | Aftersales Process Consultation | aftersales, return, refund, exchange, repair |
| ORDER_INQUIRY | Order Inquiry | order, shipping, logistics, delivery |
| UNKNOWN | Unknown Intent | None |

**Interface Methods**:

| Method Name | Return Type | Description |
|-------------|-------------|-------------|
| getDescription() | String | Get intent description |

### DifyResponse.Record

**Purpose**: Represents records returned by knowledge base retrieval

**Fields**:

| Field Name | Type | Description | Constraint |
|------------|------|-------------|------------|
| segment | Segment | Segment information | Nullable |
| score | Double | Matching score | Nullable, range 0-1 |
| id | String | Record ID | Nullable |

**Segment Sub-fields**:

| Field Name | Type | Description | Constraint |
|------------|------|-------------|------------|
| content | String | Content text | Nullable |
| document | Document | Document information | Nullable |
| keywords | List\<String\> | Keyword list | Nullable |

**Document Sub-fields**:

| Field Name | Type | Description | Constraint |
|------------|------|-------------|------------|
| name | String | Document name | Nullable |
| id | String | Document ID | Nullable |

### LLM Matching Result

**Purpose**: Represents matching result returned by LLM

**Format**:

```
Knowledge Base ID: {datasetId}
```

**Example**:

```
Knowledge Base ID: 5792187f-02e8-4b5c-a674-7efb72e8174f
```

**Special Values**:

| Value | Description |
|-------|-------------|
| DEFAULT | No suitable knowledge base matched, use default dataset |

## Data Flow

### Knowledge Base List Retrieval Flow

```
Dify API → DifyFeignClient → DifyClient → KnowledgeBaseListResponse → DifyKnowledgeBaseServiceImpl
                                                                         ↓
                                                         cachedKnowledgeBaseList (Cache)
```

**Data Transformation**:

1. Dify API returns raw JSON response
2. FeignClient automatically deserializes to KnowledgeBaseListResponse
3. DifyClient extracts data field and returns List\<KnowledgeBaseInfo\>
4. DifyKnowledgeBaseServiceImpl caches to cachedKnowledgeBaseList

### Knowledge Base Matching Flow

```
IntentType + query → findDatasetIdForIntent()
                       ↓
              ┌────────┴────────┐
              │ 1. Configuration Mapping │
              │    intentDatasetMap.get(intentType)
              └────────┬────────┘
                       ↓ (Not found)
              ┌────────┴────────┐
              │ 2. LLM Cache    │
              │    llmMatchCache.get(intentType)
              └────────┬────────┘
                       ↓ (Not found)
              ┌────────┴────────┐
              │ 3. LLM Real-Time Matching │
              │    LLMKnowledgeBaseMatcher.match()
              └────────┬────────┘
                       ↓ (Not matched)
              ┌────────┴────────┐
              │ 4. Keyword Matching │
              │    autoMatchKnowledgeBase()
              └────────┬────────┘
                       ↓ (Not matched)
              ┌────────┴────────┐
              │ 5. Default Dataset │
              │    difyClient.getDefaultDatasetId()
              └─────────────────┘
                       ↓
                  Return datasetId
```

### LLM Matching Data Preparation

**Input Data**:

| Data Item | Source | Format |
|-----------|--------|--------|
| intentType | IntentType enum | String name |
| intentDescription | IntentType.getDescription() | String |
| query | User input | String |
| knowledgeBaseList | cachedKnowledgeBaseList | Formatted string |

**Prompt Construction**:

```java
String prompt = promptTemplate
    .replace("{intentType}", intentType.name())
    .replace("{intentDescription}", intentType.getDescription())
    .replace("{query}", query)
    .replace("{knowledgeBaseList}", formatKnowledgeBaseList(knowledgeBaseList));
```

**Knowledge Base List Formatting**:

```java
private String formatKnowledgeBaseList(List<KnowledgeBaseInfo> kbList) {
    StringBuilder sb = new StringBuilder();
    for (KnowledgeBaseInfo kb : kbList) {
        sb.append("- ID: ").append(kb.getId())
          .append(" | Name: ").append(kb.getName())
          .append(" | Description: ").append(kb.getDescription() != null ? kb.getDescription() : "None")
          .append(" | Tags: ").append(kb.getTags() != null ? String.join(",", kb.getTags()) : "None")
          .append("\n");
    }
    return sb.toString();
}
```

### LLM Response Parsing

**Parsing Logic**:

```java
private String parseLlmResponse(String response) {
    if (response == null || response.isEmpty()) {
        return "DEFAULT";
    }
    
    int start = response.indexOf("Knowledge Base ID:");
    if (start == -1) {
        start = response.indexOf("Knowledge Base ID：");
    }
    
    if (start == -1) {
        Pattern pattern = Pattern.compile("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group();
        }
        return "DEFAULT";
    }
    
    int end = response.indexOf("\n", start);
    if (end == -1) {
        end = response.length();
    }
    
    String datasetId = response.substring(start + "Knowledge Base ID:".length(), end).trim();
    return datasetId.isEmpty() ? "DEFAULT" : datasetId;
}
```

## Cache Data

### Knowledge Base List Cache

**Cache Structure**:

```java
private List<KnowledgeBaseInfo> cachedKnowledgeBaseList = Collections.emptyList();
private long lastRefreshTime = 0;
```

**Cache Strategy**:

| Strategy | Description |
|----------|-------------|
| Write Timing | After refreshKnowledgeBaseList() succeeds |
| Read Timing | When knowledge base matching is needed |
| Invalidation Timing | Scheduled refresh (default 60 min) or manual refresh |
| Expiration Check | Determine if refresh is needed based on lastRefreshTime |

### LLM Matching Result Cache

**Cache Structure**:

```java
private Map<IntentType, String> llmMatchCache = new ConcurrentHashMap<>();
```

**Cache Strategy**:

| Strategy | Description |
|----------|-------------|
| Cache Key | IntentType enum |
| Cache Value | Knowledge base ID or "DEFAULT" |
| Write Timing | After LLM matching succeeds |
| Read Timing | Step 2 of findDatasetIdForIntent() |
| Invalidation Timing | When knowledge base list is refreshed |
| Capacity Limit | Unlimited (limited number of intent types) |

## Configuration Data

### application.properties

**Configuration Item Structure**:

```properties
# Dify Basic Configuration
dify.enabled=true
dify.base-url=https://api.dify.ai/v1
dify.api-key=dataset-xxx
dify.dataset-id=xxx-xxx-xxx
dify.top-k=3
dify.score-threshold=0.5
dify.knowledge-base-refresh-interval-ms=3600000

# Intent-Knowledge Base Mapping Configuration
intent.knowledge-base-mappings=

# LLM Matching Configuration
kb.matcher.llm.enabled=true
kb.matcher.llm.prompt-template=...
kb.matcher.llm.timeout-seconds=5
kb.matcher.llm.model-name=qwen-max
```

### Prompt Template Parameters

**Parameter List**:

| Parameter | Description | Example Value |
|-----------|-------------|---------------|
| {intentType} | Intent type name | PRODUCT_CONSULTATION |
| {intentDescription} | Intent description | Product Usage Consultation |
| {query} | User query content | How to use Prompt Builder |
| {knowledgeBaseList} | Formatted knowledge base list | - ID: xxx \| Name: User Guide \| ... |

### Default Configuration Values

| Configuration Item | Default Value | Description |
|--------------------|---------------|-------------|
| kb.matcher.llm.enabled | true | Enable LLM matching |
| kb.matcher.llm.timeout-seconds | 5 | Timeout 5 seconds |
| kb.matcher.llm.model-name | qwen-max | Use Qwen Max model |

## Error Data

### Exception Scenarios

| Scenario | Error Data | Handling Method |
|----------|------------|-----------------|
| LLM call timeout | No response | Degrade to keyword matching |
| LLM call failed | Exception message | Degrade to keyword matching |
| Response format error | Unparseable text | Degrade to keyword matching |
| Knowledge base list is empty | Empty list | Use default dataset |
| API key invalid | Authentication error | Service disabled |

### Log Records

**Error Log Format**:

```java
log.warn("LLM matching failed: {}", e.getMessage());
log.debug("LLM matching exception", e);
```

**Key Log Points**:

| Log Level | Scenario | Content |
|-----------|----------|---------|
| INFO | LLM matching succeeded | "LLM matched intent {} to knowledge base {}" |
| WARN | LLM matching failed | "LLM matching failed, falling back to keyword matching" |
| DEBUG | LLM call details | "LLM prompt: {}, response: {}" |
| ERROR | Severe error | Exception stack trace |