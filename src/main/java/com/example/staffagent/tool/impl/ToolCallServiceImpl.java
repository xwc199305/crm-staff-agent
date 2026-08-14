package com.example.staffagent.tool.impl;

import com.example.staffagent.context.ConversationContextHolder;
import com.example.staffagent.dify.DifyKnowledgeBaseService;
import com.example.staffagent.intent.IntentRecognizer;
import com.example.staffagent.intent.IntentType;
import com.example.staffagent.mcp.McpClient;
import com.example.staffagent.service.RagService;
import com.example.staffagent.tool.ToolCallService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToolCallServiceImpl implements ToolCallService {

    private final DifyKnowledgeBaseService difyKnowledgeBaseService;
    private final RagService ragService;
    private final IntentRecognizer intentRecognizer;
    private final McpClient mcpClient;

    @Value("${agent.api-key:}")
    private String apiKey;

    @Override
    public String callKnowledgeBase(String query, IntentType intentType) {
        log.info("Calling knowledge base for intent {}, query: {}", intentType, query);
        
        String context = ConversationContextHolder.getContext();
        log.debug("Context retrieved from ThreadLocal, length={}", context != null ? context.length() : 0);
        
        try {
            var records = difyKnowledgeBaseService.retrieveRecordsByIntent(query, intentType);
            if (records.isEmpty()) {
                log.debug("No records found in knowledge base for intent {}", intentType);
                if (context != null && !context.isEmpty()) {
                    log.info("Using mem0 context to generate fallback response");
                    return generateResponseFromContext(query, context);
                }
                return "No relevant information available";
            }

            return ragService.generate(query, records);
        } catch (Exception e) {
            log.warn("Knowledge base call failed: {}", e.getMessage());
            if (context != null && !context.isEmpty()) {
                log.info("Using mem0 context to generate fallback response after knowledge base failure");
                return generateResponseFromContext(query, context);
            }
            return "No relevant information available";
        }
    }

    private String generateResponseFromContext(String query, String context) {
        log.info("Generating response from mem0 context, context length={}", context.length());
        
        String actualApiKey = apiKey;
        if (actualApiKey == null || actualApiKey.isEmpty()) {
            actualApiKey = System.getenv("DASHSCOPE_API_KEY");
        }

        if (actualApiKey == null || actualApiKey.isEmpty()) {
            log.warn("API key not configured for context-based response generation");
            return "No relevant information available";
        }

        try {
            String prompt = "你是一个专业的客服助手。请根据以下历史对话信息，回答用户当前的问题。\n\n" +
                    "历史对话信息：\n" + context + "\n\n" +
                    "用户当前问题：" + query + "\n\n" +
                    "要求：\n" +
                    "1. 基于历史对话信息回答用户问题\n" +
                    "2. 如果历史信息中没有相关内容，请说明\n" +
                    "3. 回答要清晰、简洁";

            DashScopeChatModel model = DashScopeChatModel.builder()
                    .apiKey(actualApiKey)
                    .modelName("qwen-max")
                    .build();

            ReActAgent agent = ReActAgent.builder()
                    .name("Context-Assistant")
                    .sysPrompt("You are a helpful customer service assistant.")
                    .model(model)
                    .build();

            Msg msg = Msg.builder()
                    .textContent(prompt)
                    .build();

            Mono<Msg> responseMono = agent.call(msg);
            Msg response = responseMono.block();

            String result = response != null ? response.getTextContent() : "No relevant information available";
            log.info("Context-based response generated: {}", result.length() > 100 ? result.substring(0, 100) + "..." : result);
            return result;
        } catch (Exception e) {
            log.error("Context-based response generation failed", e);
            return "No relevant information available";
        }
    }

    @Override
    public String callWeather(String query) {
        log.info("Calling MCP getWeather tool, query: {}", query);
        return mcpClient.callTool("getWeather", Map.of("city", query));
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