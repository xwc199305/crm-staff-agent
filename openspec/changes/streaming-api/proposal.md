## Why

当前所有 chat 接口（`/chat`、`/chat-with-intent`、`/chat-react`、`/chat-react-with-intent`）均以阻塞方式返回完整响应，用户需要等待 LLM 生成完毕才能看到任何内容。对于客服场景，响应延迟可达数秒，体验不佳。流式输出（Server-Sent Events）可以让用户逐 token 看到回复，显著降低首字延迟，提升交互体验。

## What Changes

- 新增 SSE 流式聊天端点 `/api/agent/chat-stream` 和 `/api/agent/chat-with-intent-stream`，返回 `text/event-stream`
- 使用 AgentScope Java SDK 原生 `ReActAgent.stream()` 方法（返回 `Flux<Event>`）实现流式生成，无需绕过 SDK 直接调用 DashScope API
- 意图识别、知识库检索、mem0 上下文构建等前置步骤保持同步执行，仅最终 LLM 生成阶段流式输出
- 流式完成后将完整回复写入会话记忆（mem0 / ConversationMemoryManager）
- 现有非流式端点保持不变，流式与非流式并行共存

## Capabilities

### New Capabilities
- `streaming-chat`: SSE 流式聊天能力，包括流式端点定义、LLM 流式生成、流式上下文记忆写入

### Modified Capabilities
（无 — 现有非流式接口行为不变，不涉及 spec 级别变更）

## Impact

- **Controller 层**: `ReactAgentController` 新增 2 个 SSE 端点
- **Service 层**: `ReactAgentService` 新增流式方法签名，复用现有 `ReActAgent` 实例调用 `stream()` 替代 `call()`
- **LLM 调用层**: 使用 AgentScope SDK 原生 `ReActAgent.stream()` → `Flux<Event>`，`Event.getType()` 区分 `REASONING` / `AGENT_RESULT` 等事件类型
- **依赖**: 无新增依赖，`reactor-core` 已通过 AgentScope 传递依赖在 classpath 上
- **配置**: 新增 `streaming.enabled` 配置项
