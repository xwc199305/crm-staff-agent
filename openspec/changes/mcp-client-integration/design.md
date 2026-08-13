## Context

当前 `ToolCallServiceImpl` 的 `callMcpOrder` 和 `callMcpLogistics` 是空实现，`StaffAgentTools` 中注册的 `query_order` 和 `query_logistics` 工具无法执行。本地已部署 MCP (Model Context Protocol) 服务（端口 8080），提供订单和物流查询能力。需要构建 MCP client 接入该服务。

MCP 是 Anthropic 定义的标准协议，基于 JSON-RPC 2.0，支持工具发现（`tools/list`）和工具调用（`tools/call`）。项目已有 Spring Cloud OpenFeign 和 Jackson 依赖，无需引入第三方 MCP SDK。

## Goals / Non-Goals

**Goals:**
- 构建符合 MCP 协议的 Java client，通过 HTTP 传输与 MCP server 通信
- 实现 `initialize`、`tools/list`、`tools/call` 三个核心 JSON-RPC 方法
- 改造 `ToolCallServiceImpl`，使订单和物流查询真正调用 MCP server
- MCP server 地址可配置，支持超时和降级

**Non-Goals:**
- 不实现 stdio 传输（仅 HTTP）
- 不实现 MCP server 端（仅 client）
- 不实现 MCP 的 resources/prompts/sampling 等非工具能力
- 不改动 `StaffAgentTools` 的 `@Tool` 注解定义（工具签名不变）

## Decisions

### Decision 1: 传输方式 — HTTP 请求-响应（非 SSE 长连接）

**选择**：使用 HTTP POST 请求-响应模式发送 JSON-RPC 2.0 消息，每个请求独立发送、同步等待响应。

**理由**：
- 工具调用是同步的（ReAct 循环中 `toolCallService.callMcpOrder()` 返回 String），不需要 SSE 流式推送
- SSE 长连接需要 session 管理和连接保活，复杂度高且对工具调用场景无收益
- MCP server 通常也支持直接 HTTP POST 返回 JSON-RPC 响应

**替代方案**：标准 MCP SSE 传输（GET /sse 建立长连接 + POST /messages 发送请求）— 过于复杂，适用于需要 server 主动推送的场景

### Decision 2: 通信层 — 使用 HttpURLConnection + Jackson

**选择**：使用 JDK 内置 `HttpURLConnection` 发送 HTTP 请求，Jackson 序列化 JSON-RPC 消息。

**理由**：
- 与 `DashScopeStreamClient` 保持一致的通信层风格
- MCP 调用是同步的，不需要 WebClient 的响应式能力
- 无需新增依赖

**替代方案**：Spring Cloud OpenFeign — 需要定义接口和配置，对单端点 MCP 调用过于重量级

### Decision 3: 工具调用映射 — 按名称调用 MCP server 工具

**选择**：`callMcpOrder` 调用 MCP server 的 `query_order` 工具，`callMcpLogistics` 调用 `query_logistics` 工具。工具名称和参数通过配置管理。

**理由**：
- 工具名称与 `StaffAgentTools` 的 `@Tool` 注解保持一致，便于追踪
- 配置化允许 MCP server 端工具改名时不重新编译

### Decision 4: 降级策略 — MCP 不可用时返回提示信息

**选择**：MCP server 连接失败或超时时，返回 "Order inquiry service is temporarily unavailable" 而非抛出异常。

**理由**：
- ReAct 循环不应因工具调用失败而中断
- 用户能得到可理解的回复而非系统错误

## Risks / Trade-offs

- **[MCP server 端点不确定]** → 配置化 `mcp.server-url`，默认 `http://localhost:8080`，可通过配置适配不同端点路径
- **[同步阻塞调用]** → 设置超时（默认 10s），超时后降级返回提示信息
- **[MCP server 未启动]** → 降级策略保证服务可用，日志记录错误便于排查
- **[JSON-RPC 响应格式差异]** → 宽松解析 `result.content` 字段，兼容不同 MCP server 实现的返回格式
