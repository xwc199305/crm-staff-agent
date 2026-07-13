package com.example.staffagent.controller;

import com.example.staffagent.context.ChatMessage;
import com.example.staffagent.context.ConversationMemoryManager;
import com.example.staffagent.context.MemoryStats;
import com.example.staffagent.dto.ApiResponse;
import com.example.staffagent.vector.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/memory/test")
@RequiredArgsConstructor
@Slf4j
public class MemoryTestController {

    private final ConversationMemoryManager memoryManager;
    private final VectorStore vectorStore;

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        boolean connected = vectorStore.isConnected();
        return ApiResponse.success(Map.of(
                "qdrantConnected", connected,
                "description", connected ? "Qdrant vector database is connected" : "Qdrant not connected, using memory mode"
        ));
    }

    @PostMapping("/add-message")
    public ApiResponse<String> addMessage(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", "test-session-001");
        String role = request.getOrDefault("role", "user");
        String content = request.get("content");

        if (content == null || content.isEmpty()) {
            return ApiResponse.error("Content cannot be empty");
        }

        memoryManager.addMessage(sessionId, role, content);
        log.info("Added message to Memory, sessionId={}, role={}, contentLength={}", sessionId, role, content.length());

        return ApiResponse.success("Message added successfully");
    }

    @PostMapping("/retrieve")
    public ApiResponse<List<ChatMessage>> retrieveMessages(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", "test-session-001");
        String query = request.get("query");

        if (query == null || query.isEmpty()) {
            return ApiResponse.error("Query content cannot be empty");
        }

        List<ChatMessage> messages = memoryManager.retrieveRelevantMessages(sessionId, query);
        log.info("Retrieved {} relevant messages", messages.size());

        return ApiResponse.success(messages);
    }

    @PostMapping("/build-context")
    public ApiResponse<String> buildContext(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", "test-session-001");
        String query = request.get("query");

        if (query == null || query.isEmpty()) {
            return ApiResponse.error("Query content cannot be empty");
        }

        String context = memoryManager.buildContext(sessionId, query);
        log.info("Context length: {}", context.length());

        return ApiResponse.success(context);
    }

    @PostMapping("/generate-summary")
    public ApiResponse<String> generateSummary(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", "test-session-001");

        String summary = memoryManager.generateSummary(sessionId);
        log.info("Generated summary: {}", summary);

        return ApiResponse.success(summary);
    }

    @PostMapping("/stats")
    public ApiResponse<MemoryStats> getStats(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", "test-session-001");

        MemoryStats stats = memoryManager.getMemoryStats(sessionId);
        log.info("Memory stats: {}", stats);

        return ApiResponse.success(stats);
    }

    @PostMapping("/clear")
    public ApiResponse<String> clearMemory(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", "test-session-001");

        memoryManager.clearMemory(sessionId);
        log.info("Cleared Memory, sessionId={}", sessionId);

        return ApiResponse.success("Memory cleared successfully");
    }

    @PostMapping("/test-roundtrip")
    public ApiResponse<Map<String, Object>> testRoundtrip(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", "test-session-" + System.currentTimeMillis());
        String content1 = request.getOrDefault("content1", "How to use Prompt Builder to create prompts?");
        String content2 = request.getOrDefault("content2", "What is the product warranty policy?");
        String query = request.getOrDefault("query", "prompt");

        log.info("Starting Qdrant roundtrip test, sessionId={}", sessionId);

        memoryManager.addMessage(sessionId, "user", content1);
        memoryManager.addMessage(sessionId, "assistant", "Prompt Builder is a visual tool for creating and managing prompt templates.");
        memoryManager.addMessage(sessionId, "user", content2);
        memoryManager.addMessage(sessionId, "assistant", "Our product provides one year of free warranty service.");

        MemoryStats stats = memoryManager.getMemoryStats(sessionId);

        List<ChatMessage> retrieved = memoryManager.retrieveRelevantMessages(sessionId, query);

        String context = memoryManager.buildContext(sessionId, query);

        return ApiResponse.success(Map.of(
                "sessionId", sessionId,
                "stats", stats,
                "retrievedMessageCount", retrieved.size(),
                "retrievedMessages", retrieved,
                "contextLength", context.length(),
                "context", context,
                "qdrantConnected", vectorStore.isConnected()
        ));
    }
}
