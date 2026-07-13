## Context

The current project uses AgentScope Java's `ReActAgent` framework, but only treats it as a simple LLM call wrapper. The core business logic is processed through the following flow:

```
User Input → LLMIntentRecognizer(Intent Recognition) → IntentHandlerFactory(Hardcoded Tool Mapping) → ToolCallService(Tool Execution) → Return Result
```

This flow lacks core elements of the ReAct pattern:
- LLM does not participate in tool call decisions (determined by code switch-case)
- No observation and feedback loop for tool execution results
- Cannot perform multi-round reasoning and tool calls

This design adopts a **dual-mode parallel architecture**, adding a true ReAct mode without affecting the existing implementation.

## Goals / Non-Goals

**Goals:**
- Add true ReAct Agent implementation with tool registration and automatic call capabilities
- Implement complete ReAct loop: Think → Act → Observe → Re-think
- Support automatic selection of multiple tools (knowledge base retrieval, order inquiry, logistics inquiry)
- Add independent API endpoint `/api/agent/chat-react` for ReAct mode
- **Keep existing implementation completely unchanged**, both modes run in parallel

**Non-Goals:**
- Do not modify existing tool implementation logic (specific implementation of `ToolCallService`)
- Do not change existing API endpoint behavior
- Do not introduce new tool types
- Do not modify Dify knowledge base interaction protocol

## Decisions

### 1. Tool Registration Strategy

**Decision**: Use AgentScope's `@Tool` annotation + `Toolkit` for tool registration

**Rationale**: AgentScope natively supports tool registration mechanism. Methods are marked with `@Tool` annotation, managed using `Toolkit`, and then associated with `ReActAgent` via `.toolkit(toolkit)`. LLM automatically decides whether to call based on tool descriptions.

**Alternatives**:
- Custom tool call mechanism: Requires self-parsing LLM return format, high complexity
- LangChain Agent: Requires introducing new dependencies, conflicts with existing AgentScope framework

**Implementation**:
```java
// Step 1: Create tool class, mark methods with @Tool annotation
public class StaffAgentTools {
    @Tool(name = "query_knowledge_base", description = "Query knowledge base for product-related information")
    public String queryKnowledgeBase(
            @ToolParam(name = "query", description = "User query") String query) {
        return knowledgeBaseService.query(query);
    }
}

// Step 2: Create Toolkit and register tools
Toolkit toolkit = new Toolkit();
toolkit.registerTool(new StaffAgentTools());

// Step 3: Create ReActAgent and associate Toolkit
ReActAgent agent = ReActAgent.builder()
    .name("StaffAgent")
    .sysPrompt("You are an e-commerce customer service assistant...")
    .model(model)
    .toolkit(toolkit)
    .build();
```

### 2. Tool Definition Specification

**Decision**: Each tool method must contain detailed Javadoc comments as tool description

**Rationale**: AgentScope uses method's Javadoc comments as tool description. LLM determines when to call the tool based on the description. Detailed descriptions improve tool call accuracy.

**Implementation**:
```java
/**
 * Query knowledge base for product-related information
 * @param query User query
 * @return Relevant document content returned from knowledge base
 */
public String queryKnowledgeBase(String query) {
    return toolCallService.callKnowledgeBase(query, ...);
}
```

### 3. Tool Class Lifecycle Management

**Decision**: `StaffAgentTools` class uses `@Component` annotation, managed by Spring

**Rationale**: Tool classes need dependency injection of other Spring Beans (such as `ToolCallService`, `DifyKnowledgeBaseService`). Using `@Component` ensures these dependencies are properly initialized during tool registration.

**Implementation**:
```java
@Component
public class StaffAgentTools {
    
    private final ToolCallService toolCallService;
    
    @Autowired
    public StaffAgentTools(ToolCallService toolCallService) {
        this.toolCallService = toolCallService;
    }
    
    @Tool(name = "query_knowledge_base", description = "Query knowledge base for product-related information")
    public String queryKnowledgeBase(
            @ToolParam(name = "query", description = "User query") String query) {
        return toolCallService.callKnowledgeBaseForTool(query);
    }
}
```

Inject Spring-managed Bean during registration:
```java
@Autowired
private StaffAgentTools staffAgentTools;

Toolkit toolkit = new Toolkit();
toolkit.registerTool(staffAgentTools);
```

### 4. ReAct Loop Control

**Decision**: Use AgentScope's built-in maximum iteration count to control loop termination

**Rationale**: AgentScope's ReActAgent has built-in maximum iteration limit to prevent infinite loops. Default is 5 iterations, configurable.

**Implementation**:
```java
ReActAgent agent = ReActAgent.builder()
    .maxIteration(5)
    // ...
    .build();
```

### 5. Dual-Mode Parallel Architecture

**Decision**: Adopt dual-mode parallel architecture, add independent ReAct Agent implementation and API endpoint

**Rationale**: To maintain backward compatibility and avoid affecting existing business, adopt addition rather than modification. Both modes can evolve independently, users can choose which mode to use based on needs.

**Architecture Comparison**:
```
Existing Mode (Static Mapping):
User Input → LLMIntentRecognizer → IntentHandlerFactory(Hardcoded) → ToolCallService → Return Result

ReAct Mode (LLM Driven):
User Input → ReActAgent(Autonomous Decision) → Tool Call → Observe Result → Re-decision → Return Result
```

**Implementation**:
- Add `ReactAgentWithToolsServiceImpl`: True ReAct Agent implementation
- Add `StaffAgentTools`: Tool class with `@Tool` annotation marked methods
- Add `/api/agent/chat-react`: ReAct mode independent endpoint
- Existing `ReactAgentServiceImpl` and `/api/agent/chat`, `/api/agent/chat-with-intent` remain unchanged

### 6. Intent Recognition Role Positioning

**Decision**: Keep `LLMIntentRecognizer` only for existing mode, ReAct mode makes autonomous decisions

**Rationale**: Existing mode needs explicit intent recognition results for API return, while in ReAct mode LLM autonomously determines whether to call tools, no explicit intent recognition step is needed.

**Implementation**:
- Existing mode: Continue using `LLMIntentRecognizer` + `IntentHandlerFactory`
- ReAct mode: Agent makes autonomous decisions internally, no external intent recognition needed

### 7. Conversation Context Management

**Decision**: Use AgentScope's `Msg` mechanism to manage conversation context

**Rationale**: AgentScope's ReActAgent automatically maintains conversation history, including user messages, tool calls, tool return results, etc.

**Implementation**:
```java
Msg msg = Msg.builder()
    .textContent(userInput)
    .build();
Mono<Msg> response = agent.call(msg);
```

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| LLM may incorrectly select tools | Provide detailed tool descriptions, optimize tool selection logic through prompt engineering |
| Excessive tool calls cause performance issues | Set reasonable maximum iterations (default 5), monitor tool call frequency |
| Tool return result format is inconsistent | Unify tool return format, perform standardization at tool implementation layer |
| API behavior changes during migration | Keep interface signatures unchanged, gradually switch internal implementations |