package com.example.staffagent.config;

import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class McpClientConfig {

    @Bean
    public WebClient.Builder mcpWebClientBuilder(McpProperties mcpProperties) {
        return WebClient.builder()
                .baseUrl(mcpProperties.getServerUrl())
                .timeout(Duration.ofSeconds(mcpProperties.getTimeoutSeconds()));
    }

    @Bean
    public WebFluxSseClientTransport webFluxSseClientTransport(WebClient.Builder mcpWebClientBuilder) {
        return new WebFluxSseClientTransport(mcpWebClientBuilder);
    }
}