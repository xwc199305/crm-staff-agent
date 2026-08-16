package com.example.staffagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {

    private boolean enabled = true;
    private String serverUrl = "http://localhost:32790/mcp";
    private int timeoutSeconds = 10;
}
