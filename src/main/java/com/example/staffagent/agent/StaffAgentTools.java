package com.example.staffagent.agent;

import com.example.staffagent.tool.ToolCallService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaffAgentTools {

    private final ToolCallService toolCallService;

    @Tool(name = "query_knowledge_base", description = "Query knowledge base for product usage info, warranty policy, aftersales process")
    public String queryKnowledgeBase(
            @ToolParam(name = "query", description = "User query describing product info, warranty policy or aftersales process") String query) {
        log.info("Tool call: query_knowledge_base, query={}", query);
        return toolCallService.callKnowledgeBaseForTool(query);
    }

    @Tool(name = "query_order", description = "Query order status, order details and other order-related info")
    public String queryOrder(
            @ToolParam(name = "query", description = "User query describing order info to look up") String query) {
        log.info("Tool call: query_order, query={}", query);
        return toolCallService.callMcpOrder(query);
    }

    @Tool(name = "query_logistics", description = "Query logistics status, shipping tracking and other logistics info")
    public String queryLogistics(
            @ToolParam(name = "query", description = "User query describing logistics info to look up") String query) {
        log.info("Tool call: query_logistics, query={}", query);
        return toolCallService.callMcpLogistics(query);
    }
}