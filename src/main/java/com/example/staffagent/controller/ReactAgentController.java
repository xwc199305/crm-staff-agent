package com.example.staffagent.controller;

import com.example.staffagent.dto.ApiResponse;
import com.example.staffagent.dto.ChatResponse;
import com.example.staffagent.dto.IntentResult;
import com.example.staffagent.mcp.SalesforceMcpRequestContext;
import com.example.staffagent.service.ReactAgentService;
import com.example.staffagent.service.StreamingChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Slf4j
public class ReactAgentController {

    private final ReactAgentService reactAgentService;
    private final StreamingChatService streamingChatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/chat")
    public ApiResponse<String> chat(
            @RequestHeader(value = "X-CRM-ORG-DOMAIN", required = false) String orgDomain,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ChatRequest request) {
        SalesforceMcpRequestContext.set(orgDomain, authorization);
        try {
            String response = reactAgentService.call(request.getMessage());
            return ApiResponse.success(response);
        } finally {
            SalesforceMcpRequestContext.clear();
        }
    }

    @PostMapping("/chat-with-intent")
    public ApiResponse<ChatResponse> chatWithIntent(
            @RequestHeader(value = "X-CRM-ORG-DOMAIN", required = false) String orgDomain,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ChatWithIntentRequest request) {
        String userId = request.getUserId();
        if (userId == null || userId.isEmpty()) {
            userId = "default-user";
        }
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "default";
        }
        SalesforceMcpRequestContext.set(orgDomain, authorization);
        try {
            ChatResponse response = reactAgentService.chatWithIntent(userId, request.getMessage(), sessionId);
            log.info("Intent chat result: userId={}, intent={}, confidence={}",
                    userId, response.getIntentType(), response.getIntentConfidence());
            return ApiResponse.success(response);
        } finally {
            SalesforceMcpRequestContext.clear();
        }
    }

    @PostMapping("/recognize-intent")
    public ApiResponse<IntentResult> recognizeIntent(@RequestBody ChatRequest request) {
        IntentResult result = reactAgentService.recognizeIntent(request.getMessage());
        return ApiResponse.success(result);
    }

    @GetMapping("/name")
    public ApiResponse<String> getName() {
        return ApiResponse.success(reactAgentService.getName());
    }

    @PostMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody ChatRequest request) {
        log.info("Stream chat request: {}", request.getMessage());

        return streamingChatService.streamChat(request.getMessage())
                .map(chunk -> sseEvent("delta", chunk))
                .concatWith(Flux.just(sseEvent("done", "{}")))
                .onErrorResume(e -> {
                    log.error("Stream chat error", e);
                    return Flux.just(sseEvent("done", "{\"error\":\"" + e.getMessage() + "\"}"));
                });
    }

    @PostMapping(value = "/chat-with-intent-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatWithIntentStream(
            @RequestHeader(value = "X-CRM-ORG-DOMAIN", required = false) String orgDomain,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ChatWithIntentRequest request) {
        String rawUserId = request.getUserId();
        String userId = (rawUserId == null || rawUserId.isEmpty()) ? "default-user" : rawUserId;
        String rawSessionId = request.getSessionId();
        String sessionId = (rawSessionId == null || rawSessionId.isEmpty()) ? "default" : rawSessionId;
        String userInput = request.getMessage();

        log.info("Stream chat with intent: userId={}, sessionId={}, message={}", userId, sessionId, userInput);

        SalesforceMcpRequestContext.set(orgDomain, authorization);
        try {
            // The same Graph execution backs both intent endpoints so tool routing (including
            // Salesforce MCP) cannot be bypassed by the SSE endpoint.
            ChatResponse response = reactAgentService.chatWithIntent(userId, userInput, sessionId);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("intentType", response.getIntentType().name());
            metadata.put("intentDescription", response.getIntentDescription());
            metadata.put("confidence", response.getIntentConfidence());
            Map<String, Object> doneData = new HashMap<>();
            doneData.put("reply", response.getReply());
            return Flux.just(
                    sseEvent("metadata", toJson(metadata)),
                    sseEvent("delta", response.getReply()),
                    sseEvent("done", toJson(doneData)));
        } catch (Exception e) {
            log.error("Stream chat with intent error", e);
            return Flux.just(sseEvent("done", "{\"error\":\"" + e.getMessage() + "\"}"));
        } finally {
            SalesforceMcpRequestContext.clear();
        }
    }

    private ServerSentEvent<String> sseEvent(String event, String data) {
        return ServerSentEvent.<String>builder()
                .event(event)
                .data(data)
                .build();
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("Failed to serialize JSON: {}", e.getMessage());
            return "{}";
        }
    }

    @Data
    public static class ChatRequest {
        private String message;
    }

    @Data
    public static class ChatWithIntentRequest {
        private String userId;
        private String message;
        private String sessionId;
    }
}
