# DifyKnowledgeBaseService Extension Specification

## Interface Extension

**Path**: `src/main/java/com/example/staffagent/dify/DifyKnowledgeBaseService.java`

### New Method

```java
List<DifyResponse.Record> retrieveRecords(String query);
```

**Function**: Retrieve knowledge base and return raw records list

**Input**:
- query: User query

**Output**:
- List\<DifyResponse.Record\>: Knowledge base records list, returns empty list if retrieval failed or not configured

## Implementation Class Update

**Path**: `src/main/java/com/example/staffagent/dify/impl/DifyKnowledgeBaseServiceImpl.java`

### New Method Implementation

```java
@Override
public List<DifyResponse.Record> retrieveRecords(String query) {
    if (!isEnabled()) {
        log.debug("Dify knowledge base is not enabled");
        return Collections.emptyList();
    }
    
    log.info("Retrieving records from Dify knowledge base: {}", query);
    
    DifyResponse response = difyClient.retrieve(query);
    
    if (response.isSuccess() && response.getRecords() != null) {
        log.info("Dify knowledge base returned {} records", response.getRecords().size());
        return response.getRecords();
    }
    
    log.warn("Dify knowledge base retrieval failed: {}", response.getError());
    return Collections.emptyList();
}
```

### Maintain Compatibility

Original `query()` method remains unchanged for backward compatibility.