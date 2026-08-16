package com.example.staffagent.tool.impl;

import com.example.staffagent.dify.DifyKnowledgeBaseService;
import com.example.staffagent.intent.IntentRecognizer;
import com.example.staffagent.intent.IntentType;
import com.example.staffagent.mcp.McpClient;
import com.example.staffagent.service.RagService;
import com.example.staffagent.tool.ToolCallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToolCallServiceImpl implements ToolCallService {

    private static final Pattern ORDER_NUMBER_PATTERN = Pattern.compile(
            "(?:订单号|订单|order(?:\\s*(?:number|id))?)\\s*[:：#]?(\\d[\\dA-Za-z_-]*)",
            Pattern.CASE_INSENSITIVE);

    private final DifyKnowledgeBaseService difyKnowledgeBaseService;
    private final RagService ragService;
    private final IntentRecognizer intentRecognizer;
    private final McpClient mcpClient;

    private final ChatClient.Builder chatClientBuilder;

    @Override
    public String callKnowledgeBase(String query, IntentType intentType) {
        return callKnowledgeBase(query, intentType, "");
    }

    @Override
    public String callKnowledgeBase(String query, IntentType intentType, String conversationContext) {
        log.info("Calling knowledge base for intent {}, query: {}", intentType, query);
        
        try {
            var records = difyKnowledgeBaseService.retrieveRecordsByIntent(query, intentType);
            if (records.isEmpty()) {
                log.debug("No records found in knowledge base for intent {}", intentType);
                return "No relevant information available";
            }

            String queryWithHistory = conversationContext == null || conversationContext.isBlank()
                    ? query
                    : "会话上下文（仅用于理解指代）：\n" + conversationContext + "\n\n当前问题：" + query;
            return ragService.generate(queryWithHistory, records);
        } catch (Exception e) {
            log.warn("Knowledge base call failed: {}", e.getMessage());
            return "No relevant information available";
        }
    }

    @Override
    public String callWeather(String query) {
        log.info("Calling MCP getWeather tool, query: {}", query);
        return mcpClient.callTool("getWeather", Map.of("city", query));
    }

    @Override
    public String callOrder(String query) {
        return callOrder(query, "");
    }

    @Override
    public String callOrder(String query, String conversationContext) {
        String orderNumber = extractOrderNumber(query);
        if (orderNumber == null) {
            return "无法识别订单号，请提供订单号后再查询。";
        }

        String soql = "SELECT Id, OrderNumber, Account.Name "
                + "FROM Order WHERE OrderNumber = '" + orderNumber + "' LIMIT 1";
        log.info("Calling Salesforce MCP querySoql tool for orderNumber={}", orderNumber);
        String rawResult = mcpClient.callTool("querySoql", Map.of("soql", soql));
        return formatMcpResult(query, conversationContext, rawResult);
    }

    private String extractOrderNumber(String query) {
        if (query == null) {
            return null;
        }
        Matcher matcher = ORDER_NUMBER_PATTERN.matcher(query);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String formatMcpResult(String userQuery, String conversationContext, String rawResult) {
        if (rawResult == null || rawResult.isBlank() || rawResult.startsWith("MCP service")) {
            return rawResult == null ? "订单查询未返回结果。" : rawResult;
        }
        try {
            String response = chatClientBuilder.build().prompt()
                    .system("你是电商客服。会话上下文仅用于理解指代；订单事实只能依据 Salesforce MCP 查询结果，"
                            + "不得编造。用中文清晰说明订单号、状态、日期、金额、客户信息。没有记录时明确说明未找到订单。")
                    .user("会话上下文：\n" + conversationContext + "\n\n用户问题：" + userQuery
                            + "\n\nSalesforce MCP 原始结果：\n" + rawResult)
                    .call()
                    .content();
            return response == null || response.isBlank() ? rawResult : response;
        } catch (Exception e) {
            log.warn("Failed to format Salesforce MCP result, returning raw response: {}", e.getMessage());
            return rawResult;
        }
    }

    @Override
    public String callKnowledgeBaseForTool(String query) {
        log.info("Calling knowledge base for tool, query: {}", query);
        
        try {
            var intentResult = intentRecognizer.recognize(query);
            IntentType intentType = intentResult.getIntentType();
            
            if (intentType == IntentType.UNKNOWN) {
                intentType = IntentType.PRODUCT_CONSULTATION;
            }
            
            var records = difyKnowledgeBaseService.retrieveRecordsByIntent(query, intentType);
            if (records.isEmpty()) {
                log.debug("No records found in knowledge base for query {}", query);
                return "No relevant information available";
            }

            return ragService.generate(query, records);
        } catch (Exception e) {
            log.warn("Knowledge base call failed: {}", e.getMessage());
            return "No relevant information available";
        }
    }
}
