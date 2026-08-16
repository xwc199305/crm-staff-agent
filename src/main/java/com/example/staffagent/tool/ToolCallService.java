package com.example.staffagent.tool;

import com.example.staffagent.intent.IntentType;

public interface ToolCallService {
    String callKnowledgeBase(String query, IntentType intentType);
    default String callKnowledgeBase(String query, IntentType intentType, String conversationContext) {
        return callKnowledgeBase(query, intentType);
    }
    String callWeather(String query);
    String callOrder(String query);
    default String callOrder(String query, String conversationContext) {
        return callOrder(query);
    }
    String callKnowledgeBaseForTool(String query);
}
