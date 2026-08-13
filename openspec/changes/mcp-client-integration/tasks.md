## 1. 配置层

- [x] 1.1 创建 `McpProperties.java` 配置类，绑定 `mcp` 前缀，包含 `enabled`、`serverUrl`、`timeoutSeconds`、`tools.order`、`tools.logistics` 字段，提供默认值
- [x] 1.2 在 `StaffAgentApplication.java` 注册 `@EnableConfigurationProperties(McpProperties.class)`
- [x] 1.3 在 `application.yml` 新增 `mcp` 配置段（enabled、server-url、timeout-seconds、tools.order、tools.logistics）
- [x] 1.4 在 `application.yml.template` 同步新增 `mcp` 配置段占位符

## 2. MCP Client 核心实现

- [x] 2.1 创建 `McpClient.java`，实现 JSON-RPC 2.0 消息构建（`initialize`、`tools/list`、`tools/call` 方法）
- [x] 2.2 实现 HTTP 通信层（HttpURLConnection + Jackson），发送 JSON-RPC 请求并解析响应
- [x] 2.3 实现 `initialize` 握手逻辑（首次调用时自动初始化，幂等）
- [x] 2.4 实现 `callTool(String toolName, Map<String, Object> arguments)` 方法，返回工具执行结果字符串
- [x] 2.5 实现降级策略：MCP 不可用、超时、`enabled=false` 时返回提示信息，不抛异常

## 3. 改造 ToolCallServiceImpl

- [x] 3.1 注入 `McpClient` 和 `McpProperties` 到 `ToolCallServiceImpl`
- [x] 3.2 改造 `callMcpOrder`，通过 `McpClient.callTool(properties.getTools().getOrder(), ...)` 调用 MCP server
- [x] 3.3 改造 `callMcpLogistics`，通过 `McpClient.callTool(properties.getTools().getLogistics(), ...)` 调用 MCP server

## 4. 验证与提交

- [x] 4.1 `mvn clean compile` 编译验证
- [x] 4.2 启动应用，调用 `/api/agent/chat-with-intent` 测试订单查询工具链路（MCP server 不可用时验证降级）
- [ ] 4.3 按规范提交代码
