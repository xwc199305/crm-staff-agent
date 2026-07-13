package com.example.staffagent.handler.impl;

import com.example.staffagent.handler.IntentHandler;
import com.example.staffagent.intent.IntentType;
import com.example.staffagent.tool.ToolType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AftersalesProcessHandler implements IntentHandler {

    @Override
    public IntentType getIntentType() {
        return IntentType.AFTERSALES_PROCESS;
    }

    @Override
    public String handle(String query) {
        log.debug("Handling aftersales process query: {}", query);
        return "Hello! Regarding aftersales process, what specific aspects would you like to know about?";
    }

    @Override
    public ToolType getToolType() {
        return ToolType.KNOWLEDGE_BASE;
    }
}