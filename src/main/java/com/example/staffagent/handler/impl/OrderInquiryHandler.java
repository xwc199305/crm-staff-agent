package com.example.staffagent.handler.impl;

import com.example.staffagent.handler.IntentHandler;
import com.example.staffagent.intent.IntentType;
import com.example.staffagent.tool.ToolType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderInquiryHandler implements IntentHandler {

    @Override
    public IntentType getIntentType() {
        return IntentType.ORDER_INQUIRY;
    }

    @Override
    public String handle(String query) {
        log.debug("Handling order inquiry query: {}", query);
        return "Order inquiry is handled through the MCP query tool.";
    }

    @Override
    public ToolType getToolType() {
        return ToolType.MCP_ORDER;
    }
}
