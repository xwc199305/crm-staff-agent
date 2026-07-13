package com.example.staffagent.handler.impl;

import com.example.staffagent.handler.IntentHandler;
import com.example.staffagent.intent.IntentType;
import com.example.staffagent.tool.ToolType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WarrantyPolicyHandler implements IntentHandler {

    @Override
    public IntentType getIntentType() {
        return IntentType.WARRANTY_POLICY;
    }

    @Override
    public String handle(String query) {
        log.debug("Handling warranty policy query: {}", query);
        return "Hello! Regarding warranty policy, what specific aspects would you like to know about?";
    }

    @Override
    public ToolType getToolType() {
        return ToolType.KNOWLEDGE_BASE;
    }
}