package com.example.staffagent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class DashScopeStreamClient {

    private static final String DASHSCOPE_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String MODEL_NAME = "qwen-max";
    private static final String SYSTEM_PROMPT = "You are a professional knowledge QA assistant. Answer user questions based on the provided context.";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "dashscope-stream-worker");
        t.setDaemon(true);
        return t;
    });

    @Value("${agent.api-key:}")
    private String apiKey;

    public Flux<String> streamChat(String userMessage, String systemPrompt) {
        return Flux.create(sink -> {
            executor.submit(() -> {
                HttpURLConnection conn = null;
                try {
                    String actualApiKey = resolveApiKey();
                    if (actualApiKey == null || actualApiKey.isEmpty()) {
                        sink.error(new IllegalStateException("DashScope API key not configured"));
                        return;
                    }

                    String prompt = (systemPrompt != null && !systemPrompt.isEmpty())
                            ? systemPrompt + "\n\n" + userMessage
                            : userMessage;

                    String requestBody = buildRequestBody(prompt);

                    URL url = new URL(DASHSCOPE_API_URL);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + actualApiKey);
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(300000);

                    try (var os = conn.getOutputStream()) {
                        os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode != 200) {
                        String errorBody = readErrorBody(conn);
                        log.error("DashScope API error: code={}, body={}", responseCode, errorBody);
                        sink.error(new RuntimeException("DashScope API error: " + responseCode + " " + errorBody));
                        return;
                    }

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        StringBuilder fullContent = new StringBuilder();
                        boolean done = false;

                        while ((line = reader.readLine()) != null && !done) {
                            if (line.isEmpty()) continue;
                            if (!line.startsWith("data:")) continue;

                            String data = line.substring(5).trim();

                            if ("[DONE]".equals(data)) {
                                done = true;
                                break;
                            }

                            try {
                                JsonNode chunk = objectMapper.readTree(data);
                                JsonNode choices = chunk.get("choices");
                                if (choices != null && choices.isArray() && !choices.isEmpty()) {
                                    JsonNode choice = choices.get(0);
                                    JsonNode delta = choice.get("delta");
                                    if (delta != null && delta.has("content")) {
                                        String content = delta.get("content").asText();
                                        if (content != null && !content.isEmpty()) {
                                            fullContent.append(content);
                                            sink.next(content);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("Failed to parse SSE chunk: {}", data, e);
                            }
                        }

                        log.debug("Stream completed, total length={}", fullContent.length());
                        sink.complete();
                    }

                } catch (Exception e) {
                    log.error("DashScope stream error", e);
                    if (!sink.isCancelled()) {
                        sink.error(e);
                    }
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            });
        });
    }

    private String resolveApiKey() {
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }
        return System.getenv("DASHSCOPE_API_KEY");
    }

    private String buildRequestBody(String prompt) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("model", MODEL_NAME);

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);
        messages.add(systemMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);

        request.put("messages", messages);
        request.put("stream", true);

        return objectMapper.writeValueAsString(request);
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
