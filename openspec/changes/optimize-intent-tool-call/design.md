# Optimize Intent Recognition and Tool Call Logic - Detailed Design

## Architecture Design

### Overall Architecture

```
User Query → Intent Recognition → Tool Call Decision → Tool Execution → Result Processing → Response
                           ↓
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        Knowledge Base   MCP Interface  Direct Answer
        Retrieval        Call
              │            │            │
              └────────────┴────────────┘
                           ↓
                      RAG Generation (if needed)
```

### Component Responsibilities

| Component | Responsibility |
|------|------|
| ToolType | Tool type enum, defines available tools |
| IntentType | Intent type enum, associates with tool type |
| ToolCallService | Tool call service interface, defines tool call methods |
| IntentHandler | Intent handler, calls corresponding tool based on intent type |
| IntentHandlerFactory | Intent handler factory, manages handler registration and dispatch |

## Interface Design

### ToolType Enum

```java
public enum ToolType {
    KNOWLEDGE_BASE,
    MCP_ORDER,
    MCP_LOGISTICS,
    DIRECT_RESPONSE
}
```

### IntentType Extension

```java
public enum IntentType {
    PRODUCT_CONSULTATION("Product Usage Consultation", ToolType.KNOWLEDGE_BASE),
    WARRANTY_POLICY("Warranty Policy", ToolType.KNOWLEDGE_BASE),
    AFTERSALES_PROCESS("Aftersales Process", ToolType.KNOWLEDGE_BASE),
    ORDER_INQUIRY("Order Inquiry", ToolType.MCP_ORDER),
    LOGISTICS_INQUIRY("Logistics Inquiry", ToolType.MCP_LOGISTICS),
    UNKNOWN("Unknown Intent", ToolType.DIRECT_RESPONSE);

    private final String description;
    private final ToolType toolType;

    // constructor, getters
}
```

### ToolCallService Interface

```java
public interface ToolCallService {
    String callKnowledgeBase(String query, IntentType intentType);
    String callMcpOrder(String query);
    String callMcpLogistics(String query);
}
```

### IntentHandler Interface Extension

```java
public interface IntentHandler {
    IntentType getIntentType();
    String handle(String query);
    ToolType getToolType();
}
```

## Data Structures

### Tool Call Result

```java
public class ToolCallResult {
    private boolean success;
    private String content;
    private String toolName;
    private long responseTime;
}
```

## Implementation Details

### Tool Call Decision Flow

```java
public String handleIntent(String query, IntentType intentType) {
    ToolType toolType = intentType.getToolType();
    
    switch (toolType) {
        case KNOWLEDGE_BASE:
            return toolCallService.callKnowledgeBase(query, intentType);
        case MCP_ORDER:
            return toolCallService.callMcpOrder(query);
        case MCP_LOGISTICS:
            return toolCallService.callMcpLogistics(query);
        case DIRECT_RESPONSE:
        default:
            return handleDirectResponse(query, intentType);
    }
}
```

### Knowledge Base Real-Time Matching

Remove `llmMatchCache` and `intentDatasetMap` caching, match in real-time each time:

```java
public String callKnowledgeBase(String query, IntentType intentType) {
    String datasetId = findDatasetIdForIntent(intentType, query);
    
    if (datasetId == null) {
        return "No relevant information";
    }
    
    List<Record> records = difyClient.retrieve(query, datasetId);
    if (records.isEmpty()) {
        return "No relevant information";
    }
    
    return ragService.generate(query, records);
}

private String findDatasetIdForIntent(IntentType intentType, String query) {
    // 1. LLM real-time matching (no cache)
    String datasetId = llmKnowledgeBaseMatcher.match(intentType, query, cachedKnowledgeBaseList);
    if (datasetId != null && !"DEFAULT".equals(datasetId)) {
        return datasetId;
    }
    
    // 2. Keyword matching
    datasetId = autoMatchKnowledgeBase(intentType);
    if (datasetId != null) {
        return datasetId;
    }
    
    // 3. Default dataset
    return difyClient.getDefaultDatasetId();
}
```

### MCP Interface Reservation

```java
public String callMcpOrder(String query) {
    log.info("MCP order inquiry not yet implemented, query: {}", query);
    return "Order inquiry feature is under development, please try again later";
}

public String callMcpLogistics(String query) {
    log.info("MCP logistics inquiry not yet implemented, query: {}", query);
    return "Logistics inquiry feature is under development, please try again later";
}
```

## Configuration Items

No additional configuration needed, remove the following configuration items:
- `intent.knowledge-base-mappings` - No longer used

## Error Handling

1. **Knowledge base call failed**: Return "No relevant information"
2. **MCP interface not implemented**: Return friendly message "Feature is under development"
3. **Tool call timeout**: Return degraded response

## Performance Optimization

1. **Knowledge base list caching**: Keep knowledge base list caching to reduce API calls
2. **Knowledge base list periodic refresh**: Refresh every 60 minutes by default
3. **Real-time matching**: Match in real-time for each query to ensure accuracy

## Testing Points

1. **Tool call decision**: Verify different intent types call correct tools
2. **Knowledge base real-time matching**: Verify changes in knowledge base list are reflected in real-time
3. **MCP interface reservation**: Verify order inquiry returns friendly message
4. **Cache removal**: Verify functionality works correctly after removing cache