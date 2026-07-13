# KnowledgeBaseService Specification

## Interface Definition

```java
public interface DifyKnowledgeBaseService {
    Optional<String> query(String query);
    List<DifyResponse.Record> retrieveRecords(String query);
    List<DifyResponse.Record> retrieveRecordsByIntent(String query, IntentType intentType);
    List<KnowledgeBaseInfo> getKnowledgeBaseList();
    void refreshKnowledgeBaseList();
    boolean isEnabled();
}
```

## Method Description

### retrieveRecordsByIntent(String query, IntentType intentType)

Select knowledge base for retrieval based on intent type.

**Parameters**:
- `query`: User query text
- `intentType`: Intent type

**Return Value**:
- `List<DifyResponse.Record>`: Retrieved records list
- Empty list: Retrieval failed or no results

**Process**:
1. Find matched knowledge base ID based on intent type
2. If matched knowledge base found, use it for retrieval
3. If no matched knowledge base found, use default knowledge base for retrieval
4. If default knowledge base not configured, return empty list

### getKnowledgeBaseList()

Get cached knowledge base list.

**Return Value**:
- `List<KnowledgeBaseInfo>`: Knowledge base list

### refreshKnowledgeBaseList()

Manually refresh knowledge base list cache.

## Dependencies

- `DifyClient` - Dify API client
- `IntentType` - Intent type enum

## Configuration Items

| Configuration Item | Default Value | Description |
|--------------------|---------------|-------------|
| `dify.knowledge-base-refresh-interval-minutes` | 60 | Knowledge base list refresh interval |
| `intent.knowledge-base-mappings` | Empty | Intent to knowledge base mappings |