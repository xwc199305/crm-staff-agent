## Why

当前 `ToolCallServiceImpl` 中的 `callMcpOrder` 和 `callMcpLogistics` 是空实现（返回 "under development"），导致 `StaffAgentTools` 中注册的 `query_order` 和 `query_logistics` 工具无法真正执行。需要构建 MCP (Model Context Protocol) client，接入本地已部署的 MCP 服务（端口 8080），使订单查询和物流查询工具能够真正调用后端业务系统。

## What Changes

- 新增 MCP client 模块，实现 MCP 协议的 JSON-RPC 2.0 通信（基于 HTTP SSE 传输），支持 `initialize`、`tools/list`、`tools/call` 方法
- 新增 `McpClient` 配置类，从 `application.yml` 读取 MCP server 地址、超时等参数
- 改造 `ToolCallServiceImpl.callMcpOrder` 和 `callMcpLogistics`，通过 MCP client 调用远程 MCP server 的工具
- 在 `application.yml` 新增 `mcp` 配置段（server-url、timeout、enabled）
- 在 `application.yml.template` 同步新增 `mcp` 配置段占位符

## Capabilities

### New Capabilities
- `mcp-client`: MCP client 能力，负责与 MCP server 建立 SSE 连接、发现可用工具、执行工具调用，为 `StaffAgentTools` 中的订单和物流查询提供后端接入

### Modified Capabilities
<!-- 无现有 spec 需要修改 -->

## Impact

- **新增文件**：`McpClient.java`（MCP 协议客户端）、`McpProperties.java`（配置绑定类）、`McpFeignClient.java`（HTTP 传输层）
- **修改文件**：`ToolCallServiceImpl.java`（注入 McpClient，替换空实现）、`application.yml`、`application.yml.template`
- **依赖**：复用现有 Spring Cloud OpenFeign 和 Jackson，无需新增第三方依赖
- **运行时**：MCP server 需在 `mcp.server-url` 配置的地址可用（默认 `http://localhost:8080`）
