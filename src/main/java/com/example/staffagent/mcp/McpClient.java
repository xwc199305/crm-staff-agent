package com.example.staffagent.mcp;

import com.example.staffagent.config.McpProperties;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.net.URI;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class McpClient {

    private static final String CLIENT_NAME = "crm-staff-agent";

    private final McpProperties mcpProperties;
    public String callTool(String toolName, Map<String, Object> arguments) {
        if (!mcpProperties.isEnabled()) {
            log.debug("MCP disabled, skipping tool call: {}", toolName);
            return "MCP service is disabled";
        }

        McpSyncClient client = null;
        try {
            client = createClient();
            boolean exposed = client.listTools().tools().stream().anyMatch(tool -> tool.name().equals(toolName));
            if (!exposed) {
                log.warn("MCP server does not expose tool: {}", toolName);
                return "MCP tool '" + toolName + "' is not available";
            }
            McpSchema.CallToolResult result = client.callTool(
                    new McpSchema.CallToolRequest(toolName, arguments != null ? arguments : Map.of()));
            return extractTextContent(result);
        } catch (Exception e) {
            log.warn("MCP tool call failed: {} - {}", toolName, e.getMessage());
            return "MCP service is temporarily unavailable";
        } finally {
            closeQuietly(client);
        }
    }

    /**
     * A fresh official MCP SDK client is built for every request. This keeps Salesforce
     * credentials request-scoped and lets the Spring application start while MCP is offline.
     */
    private McpSyncClient createClient() {
        URI serverUri = URI.create(mcpProperties.getServerUrl());
        String baseUrl = serverUri.getScheme() + "://" + serverUri.getAuthority();
        String endpoint = serverUri.getRawPath() == null || serverUri.getRawPath().isBlank()
                ? "/mcp" : serverUri.getRawPath();

        HttpClientStreamableHttpTransport.Builder transportBuilder = HttpClientStreamableHttpTransport
                .builder(baseUrl)
                .endpoint(endpoint)
                .connectTimeout(timeout())
                .customizeRequest(request -> request.timeout(timeout()));

        SalesforceMcpRequestContext.Credentials credentials = SalesforceMcpRequestContext.get();
        if (credentials != null) {
            transportBuilder.customizeRequest(request -> request
                    .header("X-CRM-ORG-DOMAIN", credentials.orgDomain())
                    .header("Authorization", "Bearer " + credentials.accessToken()));
        }

        McpSyncClient created = io.modelcontextprotocol.client.McpClient.sync(transportBuilder.build())
                .requestTimeout(timeout())
                .initializationTimeout(timeout())
                .clientInfo(new McpSchema.Implementation(CLIENT_NAME, "1.0"))
                .build();
        try {
            created.initialize();
            log.info("Streamable HTTP MCP client initialized, serverUrl={}",
                    mcpProperties.getServerUrl());
            return created;
        } catch (Exception e) {
            closeQuietly(created);
            throw e;
        }
    }

    private Duration timeout() {
        return Duration.ofSeconds(mcpProperties.getTimeoutSeconds());
    }

    private void closeQuietly(McpSyncClient client) {
        if (client == null) {
            return;
        }
        try {
            client.closeGracefully();
        } catch (Exception e) {
            log.debug("Failed to close MCP client cleanly", e);
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
