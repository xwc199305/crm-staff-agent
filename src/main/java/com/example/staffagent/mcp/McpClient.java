package com.example.staffagent.mcp;

import com.example.staffagent.config.McpProperties;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class McpClient {

    private final McpProperties mcpProperties;
    private final WebFluxSseClientTransport transport;

    private McpSyncClient mcpSyncClient;

    @PostConstruct
    public void init() {
        if (!mcpProperties.isEnabled()) {
            log.info("MCP client is disabled, skipping initialization");
            return;
        }

        this.mcpSyncClient = McpClient.sync(transport).build();
        this.mcpSyncClient.initialize();

        log.info("MCP client initialized, serverUrl={}", mcpProperties.getServerUrl());
    }

    public String callTool(String toolName, Map<String, Object> arguments) {
        if (!mcpProperties.isEnabled()) {
            log.debug("MCP disabled, skipping tool call: {}", toolName);
            return "MCP service is disabled";
        }

        if (mcpSyncClient == null) {
            return "MCP service is temporarily unavailable";
        }

        try {
            McpSchema.CallToolResult result = mcpSyncClient.callTool(
                    new McpSchema.CallToolRequest(
                            toolName,
                            arguments != null ? arguments : Map.of()
                    )
            );
            return extractTextContent(result);
        } catch (Exception e) {
            log.error("MCP tool call failed: {} - {}", toolName, e.getMessage());
            return "MCP service is temporarily unavailable";
        }
    }

    private String extractTextContent(McpSchema.CallToolResult result) {
        if (result == null || result.content() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (var content : result.content()) {
            if (content instanceof McpSchema.TextContent textContent) {
                sb.append(textContent.text());
            }
        }
        return sb.toString();
    }
}