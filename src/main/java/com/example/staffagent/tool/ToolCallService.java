package com.example.staffagent.tool;

import com.example.staffagent.intent.IntentType;

public interface ToolCallService {
    String callKnowledgeBase(String query, IntentType intentType);
    String callWeather(String query);
    String callKnowledgeBaseForTool(String query);
}
