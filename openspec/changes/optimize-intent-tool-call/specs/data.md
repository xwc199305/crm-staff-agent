# Data Specification

## Data Structure

### ToolType Enum

**Purpose**: Define available tool types

**Enum Values**:

| Enum Value | Description |
|------------|-------------|
| KNOWLEDGE_BASE | Knowledge base retrieval tool for product consultation, warranty policy, aftersales process intents |
| MCP_ORDER | MCP order inquiry interface for order inquiry intent |
| MCP_LOGISTICS | MCP logistics inquiry interface for logistics inquiry intent |
| DIRECT_RESPONSE | Direct response without tool call |

### IntentType Extension

**Purpose**: Intent type enum with tool type association

**Fields**:

| Field Name | Type | Description | Constraint |
|------------|------|-------------|------------|
| description | String | Intent description | Not null |
| toolType | ToolType | Associated tool type | Not null |

**Enum Values**:

| Enum Value | description | toolType |
|------------|-------------|----------|
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

### ToolCallResult

**Purpose**: Tool call result

**Fields**:

| Field Name | Type | Description | Constraint |
|------------|------|-------------|------------|
| success | boolean | Whether succeeded | Not null |
| content | String | Return content | Nullable |
| toolName | String | Tool name | Nullable |
| responseTime | long | Response time (ms) | Not null |

**Example**:

```json
{
  "success": true,
  "content": "Based on knowledge base content, product usage method is as follows: ...",
  "toolName": "KNOWLEDGE_BASE",
  "responseTime": 1500
}
```

## Data Flow

### Tool Call Decision Flow

```
User Query → IntentRecognizer → IntentType
                                    ↓
                              IntentType.getToolType()
                                    ↓
                              ToolType
                                    ↓
                    ┌──────────────┼──────────────┐
                    ▼              ▼              ▼
              KNOWLEDGE_BASE   MCP_ORDER   DIRECT_RESPONSE
                    ↓              ↓              ↓
            callKnowledgeBase  callMcpOrder   Return Default Answer
                    ↓              ↓
            Knowledge Base Retrieval     MCP Interface Call
                    ↓              ↓
            RAG Generate Answer         Return Order Info
```

### Knowledge Base Real-Time Matching Flow

```
IntentType + query → findDatasetIdForIntent()
                       ↓
              ┌────────┴────────┐
              │ 1. LLM Real-Time Matching │
              │    No Cache    │
              └────────┬────────┘
                       ↓ (Not matched)
              ┌────────┴────────┐
              │ 2. Keyword Matching │
              └────────┬────────┘
                       ↓ (Not matched)
              ┌────────┴────────┐
              │ 3. Default Dataset │
              └─────────────────┘
                       ↓
                  Return datasetId
```

### Cache Strategy Changes

**Removed Cache**:

| Cache | Reason |
|-------|--------|
| llmMatchCache | Requires real-time matching, ensure accuracy |
| intentDatasetMap | Configuration mapping no longer used |

**Retained Cache**:

| Cache | Reason |
|-------|--------|
| cachedKnowledgeBaseList | Reduce API calls, knowledge base list is relatively stable |

**Cache Refresh Strategy**:

| Cache | Refresh Timing | Refresh Interval |
|-------|---------------|------------------|
| cachedKnowledgeBaseList | Scheduled refresh + Manual refresh | Default 60 minutes |

## Configuration Data

### Removed Configuration Items

| Configuration Item | Description |
|--------------------|-------------|
| intent.knowledge-base-mappings | Static mapping of intent to knowledge base, no longer used |

### Retained Configuration Items

| Configuration Item | Description | Default Value |
|--------------------|-------------|---------------|
| dify.dataset-id | Default dataset ID | - |
| kb.matcher.llm.enabled | Whether to enable LLM matching | true |
| kb.matcher.llm.timeout-seconds | LLM matching timeout | 15 |
| kb.matcher.llm.model-name | LLM model name | qwen-max |
| kb.matcher.llm.prompt-template | LLM matching prompt template | - |

## Error Data

### Exception Scenarios

| Scenario | Error Data | Handling Method |
|----------|------------|-----------------|
| Knowledge base call failed | Dify API returns 4xx/5xx error | Return "No relevant information" |
| LLM matching timeout | TimeoutException | Degrade to keyword matching |
| MCP interface not implemented | Reserved method | Return friendly prompt |
| Knowledge base list is empty | Empty list | Use default dataset |

### Log Records

**Key Log Points**:

| Log Level | Scenario | Content |
|-----------|----------|---------|
| INFO | Tool call decision | "Intent {} mapped to tool {}" |
| INFO | Knowledge base matching | "Matched intent {} to knowledge base {}" |
| INFO | MCP interface reservation | "MCP {} not implemented, query: {}" |
| WARN | Tool call failed | "Tool call failed: {}" |
| DEBUG | Matching details | "LLM match result: {}" |