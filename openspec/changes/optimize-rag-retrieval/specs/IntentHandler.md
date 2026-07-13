# IntentHandler Update Specification

## Update Scope

The following intent handlers need to be updated to use the new RAG flow:

1. `ProductConsultationHandler`
2. `WarrantyPolicyHandler`
3. `AftersalesProcessHandler`
4. `OrderInquiryHandler`

## Update Content

### Dependency Injection Extension

Each handler needs to add `RagService` dependency:

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductConsultationHandler implements IntentHandler {
    
    private final DifyKnowledgeBaseService knowledgeBaseService;
    private final RagService ragService;
    
    // ...
}
```

### handle() Method Update

**Generic Flow**:

```java
@Override
public String handle(String query) {
    log.debug("Handling {} query: {}", getIntentType(), query);
    
    // 1. Retrieve raw records from knowledge base
    List<DifyResponse.Record> records = knowledgeBaseService.retrieveRecords(query);
    
    if (!records.isEmpty()) {
        try {
            // 2. Use RAG to generate response
            String ragResult = ragService.generate(query, records);
            if (ragResult != null && !ragResult.isEmpty()) {
                log.info("RAG generated response for {} query", getIntentType());
                return ragResult;
            }
        } catch (Exception e) {
            log.error("RAG generation failed", e);
        }
    }
    
    // 3. Fall back to local knowledge base
    return fallbackAnswer(query);
}
```

### fallbackAnswer() Method

Each handler needs to keep local knowledge base as fallback:

```java
private String fallbackAnswer(String query) {
    // Original logic
}
```

## Update Order

1. ProductConsultationHandler
2. WarrantyPolicyHandler
3. AftersalesProcessHandler
4. OrderInquiryHandler