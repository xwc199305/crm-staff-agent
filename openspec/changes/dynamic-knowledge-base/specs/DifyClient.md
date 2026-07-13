# DifyClient Specification

## Interface Definition

```java
@Component
public class DifyClient {
    public DifyResponse retrieve(String query);
    public DifyResponse retrieve(String query, String datasetId);
    public KnowledgeBaseListResponse listKnowledgeBases();
    public boolean isEnabled();
}
```

## Method Description

### listKnowledgeBases()

Call Dify API to get knowledge base list.

**Return Value**:
- `KnowledgeBaseListResponse`: Knowledge base list response
- null: API call failed

**API Call**:
```
GET /v1/datasets?page=1&limit=100
Headers: Authorization: Bearer {apiKey}
```

### retrieve(String query, String datasetId)

Retrieve using specified knowledge base ID.

**Parameters**:
- `query`: User query text
- `datasetId`: Knowledge base ID

**Return Value**:
- `DifyResponse`: Retrieval result
- Failure response: API call failed

## Dependencies

- `DifyFeignClient` - Feign client interface

## Configuration Items

| Configuration Item | Default Value | Description |
|--------------------|---------------|-------------|
| `dify.base-url` | https://api.dify.ai/v1 | Dify API base URL |
| `dify.api-key` | Empty | API Key |
| `dify.top-k` | 3 | Number of results to return |
| `dify.score-threshold` | 0.5 | Score threshold |