## 1. Tool Class Creation

- [ ] 1.1 Create `StaffAgentTools` class with `@Tool` annotated methods
- [ ] 1.2 Implement knowledge base query tool with `@ToolParam` annotations
- [ ] 1.3 Implement order query tool with `@ToolParam` annotations
- [ ] 1.4 Implement logistics query tool with `@ToolParam` annotations
- [ ] 1.5 Add proper tool descriptions for LLM understanding

## 2. ReAct Agent Implementation

- [ ] 2.1 Create `ReactAgentWithToolsServiceImpl` class with explicit bean name
- [ ] 2.2 Configure `@Qualifier` for distinguishing from `ReactAgentServiceImpl`
- [ ] 2.3 Create `Toolkit` and register Spring-managed `StaffAgentTools` bean
- [ ] 2.4 Configure `.toolkit(toolkit)` in `ReActAgent.builder()`
- [ ] 2.5 Configure max iterations for ReAct loop control
- [ ] 2.6 Update system prompt to guide Agent on tool usage
- [ ] 2.7 Implement `call()` method with tool-enabled ReAct Agent

## 3. API Controller Updates

- [ ] 3.1 Add `/api/agent/chat-react` endpoint to `ReactAgentController`
- [ ] 3.2 Implement `chatReact()` method using `ReactAgentWithToolsServiceImpl`
- [ ] 3.3 Return tool usage metadata in response
- [ ] 3.4 Ensure existing endpoints (`/api/agent/chat`, `/api/agent/chat-with-intent`) unchanged

## 4. Service Interface Updates

- [ ] 4.1 Add tool-compatible methods to `ToolCallService` interface (no breaking changes)
- [ ] 4.2 Implement tool methods in `ToolCallServiceImpl`
- [ ] 4.3 Ensure existing methods remain unchanged

## 5. Testing and Verification

- [ ] 5.1 Test ReAct mode API endpoint with knowledge base queries
- [ ] 5.2 Test ReAct mode API endpoint with order/logistics queries
- [ ] 5.3 Test multi-turn conversation context in ReAct mode
- [ ] 5.4 Verify existing API endpoints still work correctly
- [ ] 5.5 Compare response quality between existing mode and ReAct mode