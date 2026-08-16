package com.example.staffagent.intent;

import com.example.staffagent.tool.ToolType;

public enum IntentType {
    PRODUCT_CONSULTATION("Product Usage Consultation", ToolType.KNOWLEDGE_BASE),
    WARRANTY_POLICY("Warranty Policy", ToolType.KNOWLEDGE_BASE),
    AFTERSALES_PROCESS("Aftersales Process", ToolType.KNOWLEDGE_BASE),
    ORDER_INQUIRY("Order Inquiry", ToolType.MCP_ORDER),
    UNKNOWN("Unknown Intent", ToolType.DIRECT_RESPONSE);

    private final String description;
    private final ToolType toolType;

    IntentType(String description, ToolType toolType) {
        this.description = description;
        this.toolType = toolType;
    }

    public String getDescription() {
        return description;
    }

    public ToolType getToolType() {
        return toolType;
    }
}
