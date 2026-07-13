## Why

Although the current project has introduced AgentScope's `ReActAgent` framework, the implementation is only a simple LLM call, lacking the core of the ReAct pattern — **LLM-driven multi-round reasoning and tool call loop**. The existing intent recognition → tool call flow is a hardcoded static mapping in code. LLM does not participate in the decision-making process, unable to achieve true Agent autonomous reasoning capability.

## What Changes

- Add true ReAct Agent implementation, allowing LLM to autonomously decide whether to call tools and which tool to call
- Register tools such as knowledge base retrieval and order inquiry to Agent, implement automatic decision-making for tool calls
- Implement complete ReAct loop: Think → Act → Observe → Re-think until generating final answer
- **Add independent endpoint** `/api/agent/chat-react` for ReAct mode, without affecting existing endpoints
- Keep existing intent recognition → tool call flow, both modes run in parallel
- Add `ReactAgentService` interface and `ReactAgentWithToolsServiceImpl` implementation class

## Capabilities

### New Capabilities
- `react-agent-with-tools`: True ReAct Agent implementation, supporting tool registration and automatic call decisions
- `agent-tool-registry`: Tool registration mechanism, unified management of available tools and their descriptions
- `react-agent-api`: ReAct mode independent API endpoint

### Modified Capabilities
- `dify-knowledge-base`: Add tool call mode methods, without affecting existing interfaces

## Impact

- **Added** `ReactAgentWithToolsServiceImpl`: True ReAct Agent implementation class
- **Added** `StaffAgentTools`: Tool class containing `@Tool` annotation marked methods
- **Added** `/api/agent/chat-react`: ReAct mode endpoint
- **`ReactAgentServiceImpl`**: Keep original implementation unchanged
- **`IntentHandlerFactory`**: Keep original hardcoded tool mapping logic unchanged
- **`ToolCallService`**: Add tool call mode methods, compatible with ReAct Agent
- **`LLMIntentRecognizer`**: Keep original implementation unchanged