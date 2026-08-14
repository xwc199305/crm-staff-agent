package com.example.staffagent.handler.impl;

import com.example.staffagent.handler.IntentHandler;
import com.example.staffagent.intent.IntentType;
import com.example.staffagent.tool.ToolCallService;
import com.example.staffagent.tool.ToolType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntentHandlerFactory {

    private final List<IntentHandler> handlers;
    private final ToolCallService toolCallService;
    private final Map<IntentType, IntentHandler> handlerMap = new EnumMap<>(IntentType.class);

    @jakarta.annotation.PostConstruct
    public void init() {
        for (IntentHandler handler : handlers) {
            handlerMap.put(handler.getIntentType(), handler);
            log.info("Registered intent handler: {}", handler.getIntentType());
        }
    }

    public IntentHandler getHandler(IntentType intentType) {
        IntentHandler handler = handlerMap.get(intentType);
        if (handler == null) {
            log.warn("No handler found for intent type: {}", intentType);
        }
        return handler;
    }

    public boolean hasHandler(IntentType intentType) {
        return handlerMap.containsKey(intentType);
    }

    public String handleWithToolCall(String query, IntentType intentType) {
        ToolType toolType = intentType.getToolType();
        log.info("Intent {} mapped to tool {}", intentType, toolType);

        return switch (toolType) {
            case KNOWLEDGE_BASE -> toolCallService.callKnowledgeBase(query, intentType);
            case MCP_WEATHER -> toolCallService.callWeather(query);
            case DIRECT_RESPONSE -> handleDirectResponse(query, intentType);
        };
    }

    private String handleDirectResponse(String query, IntentType intentType) {
        IntentHandler handler = getHandler(intentType);
        if (handler != null) {
            return handler.handle(query);
        }
        return "Sorry, I cannot understand your question.";
    }
}