## ADDED Requirements

### Requirement: MCP client 通信能力

系统 SHALL 提供 MCP client，通过 HTTP 传输向 MCP server 发送 JSON-RPC 2.0 请求，支持 `initialize`、`tools/list`、`tools/call` 三个核心方法。

#### Scenario: 成功初始化 MCP 连接
- **WHEN** MCP client 首次调用 MCP server 时
- **THEN** 系统 SHALL 先发送 `initialize` JSON-RPC 请求，收到成功响应后标记 client 为已初始化

#### Scenario: MCP server 不可达时降级
- **WHEN** MCP server 连接失败或超时（超过配置的 `mcp.timeout-seconds`）
- **THEN** 系统 SHALL 返回降级提示信息（如 "service is temporarily unavailable"），不抛出异常

#### Scenario: MCP 功能禁用
- **WHEN** `mcp.enabled` 配置为 `false`
- **THEN** 系统 SHALL 跳过 MCP 调用，直接返回降级提示信息

### Requirement: 工具调用通过 MCP 执行

系统 SHALL 将 `ToolCallServiceImpl.callMcpOrder` 和 `callMcpLogistics` 改造为通过 MCP client 调用远程 MCP server 的对应工具。

#### Scenario: 订单查询调用 MCP 工具
- **WHEN** `callMcpOrder(query)` 被调用且 MCP 可用
- **THEN** 系统 SHALL 向 MCP server 发送 `tools/call` JSON-RPC 请求，工具名为 `query_order`，参数为 `query`，返回 MCP server 的工具执行结果

#### Scenario: 物流查询调用 MCP 工具
- **WHEN** `callMcpLogistics(query)` 被调用且 MCP 可用
- **THEN** 系统 SHALL 向 MCP server 发送 `tools/call` JSON-RPC 请求，工具名为 `query_logistics`，参数为 `query`，返回 MCP server 的工具执行结果

#### Scenario: 工具名称可配置
- **WHEN** `application.yml` 中配置了 `mcp.tools.order` 和 `mcp.tools.logistics`
- **THEN** 系统 SHALL 使用配置的工具名调用 MCP server，而非硬编码

### Requirement: MCP 配置管理

系统 SHALL 通过 `application.yml` 的 `mcp` 配置段管理 MCP client 参数。

#### Scenario: 读取 MCP 配置
- **WHEN** 应用启动时
- **THEN** 系统 SHALL 从 `application.yml` 读取 `mcp.enabled`、`mcp.server-url`、`mcp.timeout-seconds`、`mcp.tools.order`、`mcp.tools.logistics` 配置项

#### Scenario: 配置缺失时使用默认值
- **WHEN** `mcp` 配置段未配置
- **THEN** 系统 SHALL 使用默认值：`enabled=true`、`server-url=http://localhost:8080`、`timeout-seconds=10`、`tools.order=query_order`、`tools.logistics=query_logistics`
