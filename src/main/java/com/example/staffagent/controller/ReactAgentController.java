package com.example.staffagent.controller;

import com.example.staffagent.dto.ApiResponse;
import com.example.staffagent.dto.ChatResponse;
import com.example.staffagent.dto.IntentResult;
import com.example.staffagent.service.ReactAgentService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
@Slf4j
public class ReactAgentController {

    private final ReactAgentService reactAgentService;
    private final ReactAgentService reactAgentWithToolsService;

    @Autowired
    public ReactAgentController(
            @Qualifier("reactAgentServiceImpl") ReactAgentService reactAgentService,
            @Qualifier("reactAgentWithToolsService") ReactAgentService reactAgentWithToolsService) {
        this.reactAgentService = reactAgentService;
        this.reactAgentWithToolsService = reactAgentWithToolsService;
    }

    @PostMapping("/chat")
    public ApiResponse<String> chat(@RequestBody ChatRequest request) {
        String response = reactAgentService.call(request.getMessage());
        return ApiResponse.success(response);
    }

    @PostMapping("/chat-with-intent")
    public ApiResponse<ChatResponse> chatWithIntent(@RequestBody ChatWithIntentRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "default";
        }
        ChatResponse response = reactAgentService.chatWithIntent(request.getMessage(), sessionId);
        log.info("Intent chat result: intent={}, confidence={}",
                response.getIntentType(), response.getIntentConfidence());
        return ApiResponse.success(response);
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

    @PostMapping("/chat-react")
    public ApiResponse<String> chatReact(@RequestBody ChatRequest request) {
        log.info("ReAct mode chat request: {}", request.getMessage());
        String response = reactAgentWithToolsService.call(request.getMessage());
        return ApiResponse.success(response);
    }

    @PostMapping("/chat-react-with-intent")
    public ApiResponse<ChatResponse> chatReactWithIntent(@RequestBody ChatWithIntentRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "default";
        }
        log.info("ReAct mode chat with intent request: sessionId={}, message={}", sessionId, request.getMessage());
        ChatResponse response = reactAgentWithToolsService.chatWithIntent(request.getMessage(), sessionId);
        log.info("ReAct mode intent chat result: intent={}, confidence={}",
                response.getIntentType(), response.getIntentConfidence());
        return ApiResponse.success(response);
    }

    @Data
    public static class ChatRequest {
        private String message;
    }

    @Data
    public static class ChatWithIntentRequest {
        private String message;
        private String sessionId;
    }
}