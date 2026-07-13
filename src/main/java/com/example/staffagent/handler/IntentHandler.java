package com.example.staffagent.handler;

import com.example.staffagent.intent.IntentType;
import com.example.staffagent.tool.ToolType;

public interface IntentHandler {
    IntentType getIntentType();
    String handle(String query);
    ToolType getToolType();
}