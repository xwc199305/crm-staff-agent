package com.example.staffagent.mcp;

import com.example.staffagent.config.McpProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@RequiredArgsConstructor
public class McpClient {

    private final McpProperties mcpProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicInteger requestId = new AtomicInteger(0);

    public String callTool(String toolName, Map<String, Object> arguments) {
        if (!mcpProperties.isEnabled()) {
            log.debug("MCP disabled, skipping tool call: {}", toolName);
            return "MCP service is disabled";
        }

        try {
            if (!initialized.get()) {
                if (!initialize()) {
                    return "MCP service is temporarily unavailable";
                }
            }

            Map<String, Object> request = buildJsonRpcRequest("tools/call", Map.of(
                    "name", toolName,
                    "arguments", arguments != null ? arguments : Map.of()
            ));

            JsonNode response = sendRequest(request);
            if (response == null) {
                return "MCP service is temporarily unavailable";
            }

            JsonNode error = response.get("error");
            if (error != null) {
                String errorMsg = error.has("message") ? error.get("message").asText() : "Unknown MCP error";
                log.warn("MCP tool call error: {} -> {}", toolName, errorMsg);
                return "Tool call failed: " + errorMsg;
            }

            JsonNode result = response.get("result");
            if (result == null) {
                return "No result from MCP tool";
            }

            return extractContent(result);
        } catch (Exception e) {
            log.error("MCP tool call failed: {} - {}", toolName, e.getMessage());
            return "MCP service is temporarily unavailable";
        }
    }

    private boolean initialize() {
        try {
            Map<String, Object> initParams = new HashMap<>();
            initParams.put("protocolVersion", "2024-11-05");
            initParams.put("capabilities", Map.of());
            initParams.put("clientInfo", Map.of(
                    "name", "crm-staff-agent",
                    "version", "1.0.0"
            ));

            Map<String, Object> request = buildJsonRpcRequest("initialize", initParams);
            JsonNode response = sendRequest(request);

            if (response != null && response.get("error") == null) {
                initialized.set(true);
                log.info("MCP client initialized successfully");
                return true;
            }

            log.warn("MCP initialize failed: {}", response);
            return false;
        } catch (Exception e) {
            log.error("MCP initialize failed: {}", e.getMessage());
            return false;
        }
    }

    private String extractContent(JsonNode result) {
        // MCP tools/call 返回格式: { "content": [{ "type": "text", "text": "..." }] }
        JsonNode content = result.get("content");
        if (content != null && content.isArray() && !content.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : content) {
                if (item.has("text")) {
                    sb.append(item.get("text").asText());
                }
            }
            return sb.toString();
        }

        // 兼容直接返回字符串的情况
        if (result.isTextual()) {
            return result.asText();
        }

        return result.toString();
    }

    private Map<String, Object> buildJsonRpcRequest(String method, Map<String, Object> params) {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", requestId.incrementAndGet());
        request.put("method", method);
        if (params != null) {
            request.put("params", params);
        }
        return request;
    }

    private JsonNode sendRequest(Map<String, Object> request) throws Exception {
        String requestBody = objectMapper.writeValueAsString(request);
        String url = mcpProperties.getServerUrl();
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += "mcp";

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(mcpProperties.getTimeoutSeconds() * 1000);
            conn.setReadTimeout(mcpProperties.getTimeoutSeconds() * 1000);

            try (var os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String errorBody = readErrorBody(conn);
                log.error("MCP server returned {}: {}", responseCode, errorBody);
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return objectMapper.readTree(sb.toString());
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String readErrorBody(HttpURLConnection conn) {
        try {
            if (conn.getErrorStream() != null) {
                return new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}
        return "";
    }
}
