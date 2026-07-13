package com.example.staffagent.service.impl;

import com.example.staffagent.context.ConversationMemoryManager;
import com.example.staffagent.dto.ChatResponse;
import com.example.staffagent.dto.IntentResult;
import com.example.staffagent.intent.IntentRecognizer;
import com.example.staffagent.intent.IntentType;
import com.example.staffagent.service.ReactAgentService;
import com.example.staffagent.tool.ToolCallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("reactAgentWithToolsService")
@RequiredArgsConstructor
@Slf4j
public class ReactAgentWithToolsServiceImpl implements ReactAgentService {

    private static final String DEFAULT_REPLY = "Sorry, we cannot understand your question. Please rephrase.";

    private final IntentRecognizer intentRecognizer;
    private final ConversationMemoryManager memoryManager;
    private final ToolCallService toolCallService;
    private final ReactAgentServiceImpl reactAgentService;

    @Value("${agent.name:React Assistant}")
    private String name;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are an e-commerce customer service assistant, responsible for answering user questions about product usage, order inquiries, logistics tracking, etc.
            
            Please refer to the following conversation history context to understand the user's question background:
            
            [Conversation History Context]
            %s
            
            You need to analyze the user's question and decide whether to call tools.
            
            Available Tools:
            1. query_knowledge_base(query): Query knowledge base for product usage info, warranty policy, aftersales process, etc.
            2. query_order(query): Query order status, order details and other order-related info.
            3. query_logistics(query): Query logistics status, shipping tracking and other logistics info.
            
            Please output in the following format:
            Thinking: Your thought process
            Tool Call: tool_name(param) OR Direct Answer: Your answer
            
            For example:
            Thinking: User is asking about product usage, need to query knowledge base
            Tool Call: query_knowledge_base(How to use Prompt Builder)
            
            Thinking: User is just greeting, no need to call tools
            Direct Answer: Hello! How can I help you?
            """;

    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile("Tool Call: (\\w+)\\(([^)]+)\\)", Pattern.DOTALL);
    private static final Pattern DIRECT_ANSWER_PATTERN = Pattern.compile("Direct Answer: (.+)", Pattern.DOTALL);

    @Override
    public String call(String userInput) {
        return callWithContext(userInput, "");
    }

    public String callWithContext(String userInput, String context) {
        log.info("Received user input (ReAct mode): {}", userInput);
        log.debug("Context length: {}", context != null ? context.length() : 0);

        List<String> history = new ArrayList<>();
        history.add("User: " + userInput);

        String currentInput = userInput;
        
        for (int iteration = 0; iteration < 3; iteration++) {
            log.info("ReAct loop iteration {}", iteration + 1);

            String prompt = buildPrompt(currentInput, history, context);
            String llmResponse = getLlmResponse(prompt);
            
            log.info("LLM response: {}", llmResponse);
            history.add("Assistant: " + llmResponse);

            Matcher toolMatcher = TOOL_CALL_PATTERN.matcher(llmResponse);
            if (toolMatcher.find()) {
                String toolName = toolMatcher.group(1);
                String toolParam = toolMatcher.group(2).trim();
                
                log.info("Recognized tool call: {}({})", toolName, toolParam);
                
                String toolResult = executeTool(toolName, toolParam);
                log.info("Tool execution result: {}", toolResult);
                history.add("Tool Result: " + toolResult);
                
                currentInput = "User asked: " + userInput + "\nTool returned: " + toolResult + "\nPlease answer the user's question based on the tool result";
            } else {
                Matcher answerMatcher = DIRECT_ANSWER_PATTERN.matcher(llmResponse);
                if (answerMatcher.find()) {
                    String finalAnswer = answerMatcher.group(1).trim();
                    log.info("ReAct mode final response: {}", finalAnswer);
                    return finalAnswer;
                } else {
                    log.info("LLM did not respond in expected format, returning content directly");
                    return llmResponse.replace("Thinking: ", "").trim();
                }
            }
        }

        log.warn("ReAct loop reached maximum iterations");
        return "Sorry, I cannot provide an accurate answer for you.";
    }

    private String buildPrompt(String userInput, List<String> history, String context) {
        String effectiveContext = (context != null && !context.isEmpty()) ? context : "No conversation history";
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(effectiveContext);
        
        StringBuilder sb = new StringBuilder();
        sb.append(systemPrompt).append("\n\n");
        
        for (String entry : history) {
            sb.append(entry).append("\n");
        }
        
        sb.append("\nPlease analyze and respond:");
        return sb.toString();
    }

    private String getLlmResponse(String prompt) {
        try {
            return reactAgentService.call(prompt);
        } catch (Exception e) {
            log.error("LLM call failed", e);
            return "Thinking: LLM call failed\nDirect Answer: Sorry, I cannot answer your question temporarily.";
        }
    }

    private String executeTool(String toolName, String param) {
        log.info("Executing tool: {}({})", toolName, param);
        
        try {
            switch (toolName) {
                case "query_knowledge_base":
                    return toolCallService.callKnowledgeBaseForTool(param);
                case "query_order":
                    return toolCallService.callMcpOrder(param);
                case "query_logistics":
                    return toolCallService.callMcpLogistics(param);
                default:
                    log.warn("Unknown tool: {}", toolName);
                    return "Unknown tool: " + toolName;
            }
        } catch (Exception e) {
            log.error("Tool execution failed: {}", e.getMessage());
            return "Tool execution failed: " + e.getMessage();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ChatResponse chatWithIntent(String userInput, String sessionId) {
        log.info("Chatting with intent recognition (ReAct mode), sessionId={}, input={}", sessionId, userInput);

        String context = memoryManager.buildContext(sessionId, userInput);
        log.debug("Context length: {}", context.length());

        IntentResult intentResult = intentRecognizer.recognize(userInput);
        IntentType intentType = intentResult.getIntentType();

        String reply;
        try {
            reply = callWithContext(userInput, context);
        } catch (Exception e) {
            log.error("ReAct agent call failed", e);
            reply = DEFAULT_REPLY;
        }

        memoryManager.addMessage(sessionId, "user", userInput);
        memoryManager.addMessage(sessionId, "assistant", reply);

        return ChatResponse.builder()
                .reply(reply)
                .intentType(intentType)
                .intentDescription(intentType.getDescription())
                .intentConfidence(intentResult.getConfidence())
                .build();
    }

    @Override
    public IntentResult recognizeIntent(String userInput) {
        return intentRecognizer.recognize(userInput);
    }
}