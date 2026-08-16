package com.example.staffagent.agent;

import com.example.staffagent.mcp.McpClient;
import com.example.staffagent.tool.ToolCallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaffAgentTools {

    private final ToolCallService toolCallService;
    private final McpClient mcpClient;

    @Tool(name = "query_knowledge_base", description = "Query knowledge base for product usage info, warranty policy, aftersales process")
    public String queryKnowledgeBase(
            @ToolParam(description = "User query describing product info, warranty policy or aftersales process") String query) {
        log.info("Tool call: query_knowledge_base, query={}", query);
        return toolCallService.callKnowledgeBaseForTool(query);
    }

    @Tool(name = "getWeather", description = "获取指定城市的天气信息")
    public String getWeather(
            @ToolParam(description = "城市名称，例如：北京、上海、广州") String city) {
        log.info("Tool call: getWeather, city={}", city);
        return mcpClient.callTool("getWeather", Map.of("city", city));
    }
}
