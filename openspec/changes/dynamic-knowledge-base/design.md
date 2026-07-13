# Dynamic Knowledge Base List Retrieval - Detailed Design

## Architecture Design

### Knowledge Base Retrieval Flow

```
Application Startup -> Retrieve Knowledge Base List -> Cache Locally -> Periodic Cache Refresh
```

### Dynamic Knowledge Base Selection Flow

```
User Input -> Intent Recognition -> Match Knowledge Base Based on Intent Type -> Retrieve from Corresponding Knowledge Base -> Return Result
```

## Component Responsibilities

### DifyFeignClient

Responsible for calling Dify API, add interface for retrieving knowledge base list.

### DifyClient

Responsible for encapsulating Dify API calls, add method for retrieving knowledge base list.

### DifyKnowledgeBaseService

Responsible for defining knowledge base service interface, add dynamic retrieval method.

### DifyKnowledgeBaseServiceImpl

Responsible for implementing knowledge base service, including:
- Retrieve and cache knowledge base list
- Select knowledge base based on intent type
- Execute dynamic retrieval

### IntentHandler Implementation Classes

Responsible for using dynamic knowledge base selection when processing user requests.

## Data Structures

### KnowledgeBaseInfo

```java
public class KnowledgeBaseInfo {
    private String id;           // Knowledge base ID
    private String name;         // Knowledge base name
    private String description;  // Knowledge base description
    private List<String> tags;   // Tag list
}
```

### KnowledgeBaseListResponse

```java
public class KnowledgeBaseListResponse {
    private List<KnowledgeBaseInfo> data;  // Knowledge base list
    private boolean has_more;              // Has more
    private int limit;                     // Items per page
    private int total;                     // Total count
    private int page;                      // Current page
}
```

## Configuration Items

| Item | Default | Description |
|--------|--------|------|
| `dify.knowledge-base-refresh-interval-minutes` | 60 | Knowledge base list refresh interval (minutes) |
| `intent.knowledge-base-mappings` | Empty | Mapping between intent types and knowledge base IDs, format: INTENT_TYPE=dataset_id |

## API Calls

### Retrieve Knowledge Base List

```
GET /v1/datasets?page=1&limit=20
Headers: Authorization: Bearer {apiKey}
```

## Mapping Rules

Establish mapping between intent types and knowledge bases through configuration file:

```properties
intent.knowledge-base-mappings=PRODUCT_CONSULTATION=xxx-xxx,WARRANTY_POLICY=yyy-yyy
```

If no matching mapping exists, use the default knowledge base (`dify.dataset-id`).

## Cache Strategy

- Retrieve knowledge base list once at application startup
- Periodic refresh (default 60 minutes)
- Support manual trigger refresh

## Error Handling

- API call failure: Use cached knowledge base list
- No matching knowledge base: Use default knowledge base
- Default knowledge base not configured: Return empty result