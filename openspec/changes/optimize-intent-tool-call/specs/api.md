# API Specification

## Internal Interfaces

### ToolType Enum

**Function**: Define available tool types

**Enum Values**:

| Enum Value | Description |
|------------|-------------|
| KNOWLEDGE_BASE | Knowledge base retrieval tool |
| MCP_ORDER | MCP order inquiry interface |
| MCP_LOGISTICS | MCP logistics inquiry interface |
| DIRECT_RESPONSE | Direct response (no tool call required) |

### IntentType Extension

**Function**: Intent type enum with tool type association

**Enum Values**:

| Enum Value | Description | Tool Type |
|------------|-------------|-----------|
| PRODUCT_CONSULTATION | Product Usage Consultation | KNOWLEDGE_BASE |
| WARRANTY_POLICY | Warranty Policy Consultation | KNOWLEDGE_BASE |
| AFTERSALES_PROCESS | Aftersales Process Consultation | KNOWLEDGE_BASE |
| ORDER_INQUIRY | Order Inquiry | MCP_ORDER |
| LOGISTICS_INQUIRY | Logistics Inquiry | MCP_LOGISTICS |
| UNKNOWN | Unknown Intent | DIRECT_RESPONSE |

**Methods**:

| Method Name | Return Type | Description |
|-------------|-------------|-------------|
| getDescription() | String | Get intent description |
| getToolType() | ToolType | Get associated tool type |

### ToolCallService Interface

**Function**: Tool call service, unified management of various tool calls

#### callKnowledgeBase Method

**Parameters**:

| Parameter | Type | Description |
|-----------|------|-------------|
| query | String | User query content |
| intentType | IntentType | Intent type |

**Return Value**:

| Return Type | Description |
|-------------|-------------|
| String | Tool call result |

**Logic**:
1. Match knowledge base in real-time based on intent type
2. Call knowledge base retrieval API
3. If there is a result, call RAG to generate answer
4. If no result, return "No relevant information"

#### callMcpOrder Method

**Parameters**:

| Parameter | Type | Description |
|-----------|------|-------------|
| query | String | User query content |

**Return Value**:

| Return Type | Description |
|-------------|-------------|
| String | Tool call result |

**Logic**:
1. Reserve MCP order inquiry interface call
2. Currently return friendly prompt "Feature in development"

#### callMcpLogistics Method

**Parameters**:

| Parameter | Type | Description |
|-----------|------|-------------|
| query | String | User query content |

**Return Value**:

| Return Type | Description |
|-------------|-------------|
| String | Tool call result |

**Logic**:
1. Reserve MCP logistics inquiry interface call
2. Currently return friendly prompt "Feature in development"

### IntentHandler Interface Extension

**Function**: Intent handler interface, extend tool type get method

#### getToolType Method

**Parameters**: None

**Return Value**:

| Return Type | Description |
|-------------|-------------|
| ToolType | Tool type associated with current handler |

### IntentHandlerFactory Interface Extension

**Function**: Intent handler factory, manage tool call decisions

#### handleWithToolCall Method

**Parameters**:

| Parameter | Type | Description |
|-----------|------|-------------|
| query | String | User query content |
| intentType | IntentType | Intent type |

**Return Value**:

| Return Type | Description |
|-------------|-------------|
| String | Processing result |

**Logic**:
1. Get tool type based on intent type
2. Call corresponding tool
3. Return processing result

## Configuration Interfaces

### application.properties

Remove the following configuration items:
- `intent.knowledge-base-mappings` - No longer used

Retain the following configuration items:
- `dify.dataset-id` - Default dataset configuration
- `kb.matcher.llm.*` - LLM matching related configuration

## Error Handling Interfaces

### Exception Types

| Exception | Scenario | Handling Method |
|-----------|----------|-----------------|
| Knowledge base call failed | Dify API returns error | Return "No relevant information" |
| MCP interface not implemented | Order/logistics inquiry | Return friendly prompt |
| Tool call timeout | LLM matching timeout | Degrade to keyword matching |

### Degradation Chain

```
LLM Matching Failed → Keyword Matching → Default Dataset → "No relevant information"
```

## Data Flow

### Tool Call Data Flow

```
User Query → IntentRecognizer → IntentType → ToolType
                                                ↓
                              ┌────────────────┴────────────────┐
                              ▼                                 ▼
                        ToolCallService                    Direct Response
                              ↓
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        callKnowledgeBase  callMcpOrder  callMcpLogistics
              ↓               ↓               ↓
        Dify API          MCP Interface    MCP Interface
              ↓               ↓               ↓
        RagService        Order Info       Logistics Info
              ↓               ↓               ↓
        Generate Answer   Return Result    Return Result
```

### Knowledge Base Real-Time Matching Data Flow

```
IntentType + query → findDatasetIdForIntent()
                       ↓
              ┌────────┴────────┐
              │ 1. LLM Real-Time Matching │
              │    llmKnowledgeBaseMatcher.match()
              └────────┬────────┘
                       ↓ (Not matched)
              ┌────────┴────────┐
              │ 2. Keyword Matching │
              │    autoMatchKnowledgeBase()
              └────────┬────────┘
                       ↓ (Not matched)
              ┌────────┴────────┐
              │ 3. Default Dataset │
              │    difyClient.getDefaultDatasetId()
              └─────────────────┘
                       ↓
                  Return datasetId
```