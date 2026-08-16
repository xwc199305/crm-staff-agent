package com.example.staffagent.mcp;

import com.example.staffagent.config.McpProperties;
import io.modelcontextprotocol.json.McpJsonMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class McpClientTest {

    @Test
    void mcpJsonMapperProviderIsAvailableForStreamableHttpTransport() {
        assertThat(McpJsonMapper.getDefault()).isNotNull();
    }

    @Test
    void disabledClientDoesNotAttemptToConnect() {
        McpProperties properties = new McpProperties();
        properties.setEnabled(false);

        McpClient client = new McpClient(properties);

        assertThat(client.callTool("query", null)).isEqualTo("MCP service is disabled");
    }
}
