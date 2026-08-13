package com.example.staffagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {

    private boolean enabled = true;
    private String serverUrl = "http://localhost:8080";
    private int timeoutSeconds = 10;
    private Tools tools = new Tools();

    @Data
    public static class Tools {
        private String order = "query_order";
        private String logistics = "query_logistics";
    }
}
