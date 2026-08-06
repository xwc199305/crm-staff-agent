## Context

当前项目所有 chat 接口以阻塞方式返回完整响应。LLM 调用通过 AgentScope Java 的 `ReActAgent.call()` 返回 `Mono<Msg>`，调用 `.block()` 等待完整响应。项目使用 `spring-boot-starter-web`（Spring MVC），但 Reactor Core 已在 classpath 上（AgentScope 依赖）。

**关键发现**: AgentScope Java SDK 1.0.11 的 `Agent` 接口继承自 `StreamableAgent`，`ReActAgent` 通过继承链自动获得 `stream()` 方法，返回 `Flux<Event>`。`Event` 包含 `EventType`（`REASONING` / `AGENT_RESULT` / `TOOL_RESULT` / `SUMMARY` 等）、`Msg` 和 `isLast()` 标记。`StreamOptions` 可配置增量模式、推理/总结 chunk 过滤等。

聊天流程：用户输入 → 意图识别（LLM，同步）→ 知识库检索（Dify API，同步）→ RAG 生成（LLM，同步）→ 返回完整响应。其中 RAG 生成阶段耗时最长（2-5秒），是流式化的主要收益点。

## Goals / Non-Goals

**Goals:**
- 新增 SSE 流式端点，将 LLM 生成阶段改为逐 token 输出
- 复用现有意图识别、知识库检索、mem0 上下文构建链路
- 流式完成后自动写入会话记忆
- 现有非流式端点完全不受影响

**Non-Goals:**
- 不修改 AgentScope Java 库本身的 `ReActAgent` 实现（仅使用其已有的 `stream()` API）
- 不对意图识别阶段做流式化（该阶段需要完整结果才能路由）
- 不实现客户端断线重连/续传
- 不修改 ReAct with tools 模式的多轮推理循环

## Decisions

### Decision 1: 流式传输方式 — `Flux<String>` + `text/event-stream`

**选择:** Controller 方法返回 `Flux<String>`，设置 `produces = MediaType.TEXT_EVENT_STREAM_VALUE`

**理由:**
- Spring MVC 在 Reactor Core 存在时原生支持 `Flux` 返回类型自动转为 SSE
- 与项目现有的 Reactor 风格一致（服务层已大量使用 `Mono`）
- 无需引入 `SseEmitter` 的手动管理（onCompletion / onTimeout / onError 回调）

**备选方案:**
- `SseEmitter`: 需要手动管理生命周期回调，代码更复杂
- `StreamingResponseBody`: 面向字节流，不适合文本 token 场景

### Decision 2: LLM 流式调用 — 使用 AgentScope SDK 原生 `ReActAgent.stream()`

**选择:** 直接调用现有 `ReActAgent` 实例的 `stream()` 方法，获取 `Flux<Event>` 流，按 `EventType` 过滤并提取文本内容

**理由:**
- SDK 1.0.11 的 `Agent` 接口继承 `StreamableAgent`，`ReActAgent` 天然支持 `stream()`
- `stream()` 返回 `Flux<Event>`，`Event.getType()` 区分 `REASONING`（推理 chunk）、`AGENT_RESULT`（最终回复）等
- 无需自行解析 DashScope SSE 协议，SDK 内部已处理
- 复用现有 `ReActAgent` 实例和 `DashScopeChatModel` 配置，零额外依赖
- 可选流式输出 ReAct 推理过程（`StreamOptions.includeReasoningChunk`），增强可观测性

**使用方式:**
```java
ReActAgent agent = getAgent(); // 复用现有实例
Msg msg = Msg.builder().textContent(prompt).build();

StreamOptions options = StreamOptions.builder()
        .incremental(true)
        .includeReasoningChunk(false)  // 仅输出最终回复，不输出推理过程
        .build();

Flux<Event> eventFlux = agent.stream(msg, options);

// 过滤 AGENT_RESULT 事件，提取文本
Flux<String> textFlux = eventFlux
        .filter(e -> e.getType() == EventType.AGENT_RESULT)
        .map(e -> e.getMessage().getTextContent())
        .filter(text -> text != null && !text.isEmpty());
```

**备选方案（已否决）:**
- 直接调用 DashScope OpenAI 兼容 API + 手动 SSE 解析：需自行处理 HTTP 连接、SSE 行解析、错误重试，且无法复用 SDK 的 ReAct 推理循环
- 引入 `spring-boot-starter-webflux` 用 `WebClient`：增加依赖重量，且 SDK 已提供原生流式支持，无必要

### Decision 3: 流式事件格式

**选择:** 三类 SSE 事件，通过 `event` 字段区分

```
event: metadata
data: {"intentType":"AFTERSALES_PROCESS","confidence":0.95}

event: delta
data: 根据

event: delta
data: 我们的

event: done
data: {"reply":"完整回复内容..."}
```

**理由:**
- `metadata` 事件让前端尽早获取意图信息（并行于流式生成）
- `delta` 事件携带文本片段，前端逐字拼接
- `done` 事件携带完整回复，供前端做最终校准

### Decision 4: 流式服务层架构 — 新增 `StreamingChatService`

**选择:** 新建 `StreamingChatService` 接口和 `StreamingChatServiceImpl`，封装 `ReActAgent.stream()` 调用 + 事件过滤 + 文本提取

**理由:**
- 不污染现有 `RagService` / `ReactAgentService` 接口（非流式场景无需感知 streaming 逻辑）
- `StreamingChatService` 复用 `RagServiceImpl` 的 prompt 构建、知识库上下文合并逻辑
- 封装 `Flux<Event>` → `Flux<String>` 的转换，屏蔽 SDK Event 类型细节
- 职责单一，便于测试

### Decision 5: 会话记忆写入时机

**选择:** 在 `Flux` 的 `doOnComplete()` 回调中，将累积的完整回复写入 `ConversationMemoryManager`

**理由:**
- 必须等所有 chunk 收集完毕才能得到完整回复
- `doOnComplete` 在流正常结束时触发，不会阻塞流式输出
- 若流中断（客户端断开），`doOnCancel` 中不写入不完整的记忆

## Risks / Trade-offs

- **[SDK stream() 行为未验证]** AgentScope SDK 1.0.11 的 `stream()` 虽然存在于接口中，但实际流式行为（chunk 粒度、Event 类型时序）需实测验证 → 先用简单 prompt 测试 `stream()` 输出，确认 `AGENT_RESULT` 事件确实逐 chunk 到达
- **[DashScope API 限流]** streaming 请求与非流式请求共享 QPS 配额 → 流式端点复用现有 API Key，暂不做额外限流
- **[客户端断连导致记忆丢失]** 用户关闭浏览器时流被 cancel → 可接受，非完整对话不写入记忆是合理行为
- **[SSE 代理兼容性]** 某些反向代理可能缓冲 SSE → 文档中注明需禁用 Nginx `proxy_buffering`
