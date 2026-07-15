package com.example.staffagent.context;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class Mem0Client {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mem0.enabled:false}")
    private boolean enabled;

    @Value("${mem0.server-url:http://localhost:8283}")
    private String serverUrl;

    @Value("${mem0.infer:true}")
    private boolean infer;

    @Value("${mem0.top-k:5}")
    private int topK;

    public Mem0Client(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(30))
                .build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void addMemory(String userId, String content, String role) {
        if (!enabled) {
            return;
        }

        try {
            String url = serverUrl + "/api/memories";

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("messages", content);
            requestBody.put("user_id", userId);
            requestBody.put("infer", infer);

            log.info("Sending add memory request to {}: {}", url, requestBody);

            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

            if (response != null && "success".equals(response.get("status"))) {
                log.info("Memory added to mem0 server successfully, userId={}, contentLength={}", userId, content.length());
            } else {
                log.error("Failed to add memory to mem0 server, response: {}", response);
            }
        } catch (Exception e) {
            log.error("Failed to add memory to mem0 server: {}", e.getMessage());
        }
    }

    public void addMessageMemory(String userId, String userMessage, String assistantReply) {
        if (!enabled) {
            return;
        }

        try {
            String url = serverUrl + "/api/memories";

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", userMessage));
            messages.add(Map.of("role", "assistant", "content", assistantReply));

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("messages", messages);
            requestBody.put("user_id", userId);
            requestBody.put("infer", infer);

            log.debug("Sending add message memory request to {}: {}", url, requestBody);

            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

            if (response != null && "success".equals(response.get("status"))) {
                log.debug("Message memory added to mem0 server successfully, userId={}", userId);
            } else {
                log.error("Failed to add message memory to mem0 server, response: {}", response);
            }
        } catch (Exception e) {
            log.error("Failed to add message memory to mem0 server: {}", e.getMessage());
        }
    }

    public List<ChatMessage> searchMemories(String userId, String query) {
        if (!enabled) {
            return new ArrayList<>();
        }

        try {
            String url = serverUrl + "/api/memories/search";

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("query", query);
            requestBody.put("user_id", userId);
            requestBody.put("top_k", topK);

            log.debug("Sending search memory request to {}: {}", url, requestBody);

            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

            if (response != null && "success".equals(response.get("status"))) {
                log.debug("Search response: {}", response);
                Object resultsObj = response.get("results");
                if (resultsObj instanceof Map) {
                    Map<String, Object> resultsMap = (Map<String, Object>) resultsObj;
                    Object memoriesArrayObj = resultsMap.get("results");
                    if (memoriesArrayObj instanceof List) {
                        List<?> memoriesArray = (List<?>) memoriesArrayObj;
                        List<ChatMessage> messages = new ArrayList<>();
                        for (Object item : memoriesArray) {
                            if (item instanceof Map) {
                                Map<String, Object> memoryNode = (Map<String, Object>) item;
                                ChatMessage message = ChatMessage.builder()
                                        .id(memoryNode.containsKey("id") ? String.valueOf(memoryNode.get("id")) : "")
                                        .userId(memoryNode.containsKey("user_id") ? String.valueOf(memoryNode.get("user_id")) : userId)
                                        .role(memoryNode.containsKey("role") ? String.valueOf(memoryNode.get("role")) : "user")
                                        .content(memoryNode.containsKey("memory") ? String.valueOf(memoryNode.get("memory")) : "")
                                        .build();
                                messages.add(message);
                            }
                        }
                        log.debug("Retrieved {} memories from mem0 server for userId={}", messages.size(), userId);
                        return messages;
                    }
                }
            } else {
                log.error("Failed to search memories from mem0 server, response: {}", response);
            }
        } catch (Exception e) {
            log.error("Failed to search memories from mem0 server: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    public List<ChatMessage> getUserMemories(String userId) {
        if (!enabled) {
            return new ArrayList<>();
        }

        try {
            String url = serverUrl + "/api/memories/" + userId;

            log.debug("Getting memories for user: {}", userId);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && "success".equals(response.get("status"))) {
                log.debug("Get user memories response: {}", response);
                Object memoriesObj = response.get("memories");
                if (memoriesObj instanceof Map) {
                    Map<String, Object> memoriesMap = (Map<String, Object>) memoriesObj;
                    Object memoriesArrayObj = memoriesMap.get("results");
                    if (memoriesArrayObj instanceof List) {
                        List<?> memoriesArray = (List<?>) memoriesArrayObj;
                        List<ChatMessage> messages = new ArrayList<>();
                        for (Object item : memoriesArray) {
                            if (item instanceof Map) {
                                Map<String, Object> memoryNode = (Map<String, Object>) item;
                                ChatMessage message = ChatMessage.builder()
                                        .id(memoryNode.containsKey("id") ? String.valueOf(memoryNode.get("id")) : "")
                                        .userId(memoryNode.containsKey("user_id") ? String.valueOf(memoryNode.get("user_id")) : userId)
                                        .role(memoryNode.containsKey("role") ? String.valueOf(memoryNode.get("role")) : "user")
                                        .content(memoryNode.containsKey("memory") ? String.valueOf(memoryNode.get("memory")) : "")
                                        .build();
                                messages.add(message);
                            }
                        }
                        log.debug("Retrieved {} memories for userId={}", messages.size(), userId);
                        return messages;
                    }
                }
            } else {
                log.error("Failed to get user memories from mem0 server, response: {}", response);
            }
        } catch (Exception e) {
            log.error("Failed to get user memories from mem0 server: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    public void deleteUserMemories(String userId) {
        if (!enabled) {
            return;
        }

        try {
            String url = serverUrl + "/api/memories/user/" + userId;

            log.debug("Deleting memories for user: {}", userId);

            restTemplate.delete(url);

            log.debug("Deleted all memories for userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to delete user memories from mem0 server: {}", e.getMessage());
        }
    }

    public boolean isConnected() {
        if (!enabled) {
            return false;
        }

        try {
            String url = serverUrl + "/api/health";

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            boolean healthy = response != null && "healthy".equals(response.get("status"));
            if (!healthy) {
                log.warn("mem0 server health check failed, response: {}", response);
            }
            return healthy;
        } catch (Exception e) {
            log.warn("mem0 server not reachable: {}", e.getMessage());
            return false;
        }
    }
}