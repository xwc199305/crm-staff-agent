## 1. 配置与 SDK 验证

- [x] 1.1 在 `application.properties` 中新增 `streaming.enabled` 配置项
- [x] 1.2 确认 `reactor-core` 依赖在 classpath 上（通过 AgentScope 传递依赖）
- [x] 1.3 编写临时测试：用简单 prompt 调用 `ReActAgent.stream(Msg, StreamOptions)`，打印 `Flux<Event>` 输出，验证 `AGENT_RESULT` 事件确实逐 chunk 到达

## 2. StreamingChatService 服务层

- [x] 2.1 创建 `StreamingChatService` 接口，定义 `Flux<String> streamChat(String prompt)` 和 `Flux<String> streamChat(String query, List<DifyResponse.Record> records)` 方法
- [x] 2.2 创建 `StreamingChatServiceImpl` 实现，复用现有 `ReActAgent` 实例（通过 `ReactAgentServiceImpl.getAgent()`）
- [x] 2.3 实现 `stream()` 调用：构建 `StreamOptions`（incremental=true, includeReasoningChunk=false），调用 `agent.stream(msg, options)` 获取 `Flux<Event>`
- [x] 2.4 实现 `Flux<Event>` → `Flux<String>` 转换：过滤 `EventType.AGENT_RESULT` 事件，提取 `event.getMessage().getTextContent()`，跳过空内容
- [x] 2.5 复用 `RagServiceImpl` 的 prompt 构建和上下文合并逻辑（从 `ConversationContextHolder` 读取历史上下文）

## 3. Controller 层 SSE 端点

- [x] 3.1 在 `ReactAgentController` 中新增 `POST /api/agent/chat-stream` 端点，返回 `Flux<ServerSentEvent<String>>`
- [x] 3.2 在 `ReactAgentController` 中新增 `POST /api/agent/chat-with-intent-stream` 端点
- [x] 3.3 `chat-with-intent-stream` 实现：同步执行意图识别和知识库检索 → 发 `metadata` 事件 → 调用 `StreamingChatService` 发 `delta` 事件 → 流结束发 `done` 事件
- [x] 3.4 在 `doOnComplete` 回调中累积完整回复并调用 `memoryManager.addMessagePair()` 写入记忆
- [x] 3.5 在 `doFinally` 中调用 `ConversationContextHolder.clearContext()` 清理 ThreadLocal
- [x] 3.6 处理 userId / sessionId 默认值逻辑（与现有非流式端点一致）

## 4. 编译验证与测试

- [x] 4.1 执行 `mvn clean compile` 确认编译通过
- [x] 4.2 启动应用，使用 curl 测试 `/api/agent/chat-stream` 端点 SSE 输出
- [x] 4.3 测试 `/api/agent/chat-with-intent-stream` 端点，验证 metadata → delta → done 事件顺序
- [x] 4.4 验证流式完成后 mem0 中写入了完整对话记忆
- [x] 4.5 验证现有非流式端点（`/chat`、`/chat-with-intent`）功能不受影响
