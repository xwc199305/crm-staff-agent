package com.example.staffagent.controller;

import com.example.staffagent.dto.ApiResponse;
import com.example.staffagent.dto.ChatResponse;
import com.example.staffagent.service.ReactAgentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Workflow Agent Controller
 * 基于 workflow 风格的手动思考-行动循环实现（非 ReActAgent SDK）
 */
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Slf4j
public class WorkflowAgentController {

    private final ReactAgentService reactAgentWithToolsService;

    @PostMapping("/chat")
    public ApiResponse<String> chat(@RequestBody ChatRequest request) {
        log.info("Workflow mode chat request: {}", request.getMessage());
        String response = reactAgentWithToolsService.call(request.getMessage());
        return ApiResponse.success(response);
    }

    @PostMapping("/chat-with-intent")
    public ApiResponse<ChatResponse> chatWithIntent(@RequestBody ChatWithIntentRequest request) {
        String userId = request.getUserId();
        if (userId == null || userId.isEmpty()) {
            userId = "default-user";
        }
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "default";
        }
        log.info("Workflow mode chat with intent request: userId={}, sessionId={}, message={}", userId, sessionId, request.getMessage());
        ChatResponse response = reactAgentWithToolsService.chatWithIntent(userId, request.getMessage(), sessionId);
        log.info("Workflow mode intent chat result: userId={}, intent={}, confidence={}",
                userId, response.getIntentType(), response.getIntentConfidence());
        return ApiResponse.success(response);
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
