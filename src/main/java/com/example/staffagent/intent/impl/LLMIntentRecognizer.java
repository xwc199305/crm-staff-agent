package com.example.staffagent.intent.impl;

import com.example.staffagent.dto.IntentResult;
import com.example.staffagent.intent.IntentType;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LLMIntentRecognizer {

    private ReActAgent agent;

    @Value("${agent.api-key:}")
    private String apiKey;

    private static final String INTENT_PROMPT = """
            You are an e-commerce customer service intent recognition expert. Analyze user input and classify it into one of the following intents:
            
            Intent List:
            1. PRODUCT_CONSULTATION - Product Usage Consultation: Users ask about product usage, features, operation guides, etc.
            2. WARRANTY_POLICY - Warranty Policy: Users ask about warranty terms, duration, coverage, etc.
            3. AFTERSALES_PROCESS - Aftersales Process: Users ask about returns, refunds, exchanges, repairs, etc.
            4. ORDER_INQUIRY - Order Inquiry: Users check order status, logistics info, shipping status, etc.
            5. UNKNOWN - Unrecognized intent
            
            Please output strictly in the following format:
            INTENT: <intent_type>
            CONFIDENCE: <0.0-1.0>
            KEYWORDS: <comma-separated keywords>
            
            User Input:
            %s
            """;

    private ReActAgent getAgent() {
        if (agent == null) {
            String actualApiKey = apiKey;
            if (actualApiKey == null || actualApiKey.isEmpty()) {
                actualApiKey = System.getenv("DASHSCOPE_API_KEY");
            }

            if (actualApiKey == null || actualApiKey.isEmpty()) {
                log.warn("API key not configured, LLM intent recognition will not work");
                return null;
            }

            DashScopeChatModel model = DashScopeChatModel.builder()
                    .apiKey(actualApiKey)
                    .modelName("qwen-max")
                    .build();

            agent = ReActAgent.builder()
                    .name("IntentRecognizer")
                    .sysPrompt("You are a professional intent recognition assistant. Output results strictly in the required format.")
                    .model(model)
                    .build();
        }
        return agent;
    }

    public IntentResult recognize(String query) {
        ReActAgent intentAgent = getAgent();
        if (intentAgent == null) {
            log.warn("LLM intent recognition skipped due to missing API key");
            return null;
        }

        try {
            String prompt = String.format(INTENT_PROMPT, query);
            Msg msg = Msg.builder()
                    .textContent(prompt)
                    .build();

            Mono<Msg> responseMono = intentAgent.call(msg);
            Msg response = responseMono.block();

            if (response == null || response.getTextContent() == null) {
                log.warn("LLM intent recognition returned null");
                return null;
            }
            log.info("LLM intent recognition response: {}", response.getTextContent());
            return parseIntentResponse(response.getTextContent(), query);
        } catch (Exception e) {
            log.error("LLM intent recognition failed", e);
            return null;
        }
    }

    private IntentResult parseIntentResponse(String response, String query) {
        try {
            String intentStr = extractField(response, "INTENT:");
            String confidenceStr = extractField(response, "CONFIDENCE:");
            String keywordsStr = extractField(response, "KEYWORDS:");
            IntentType intentType = parseIntentType(intentStr);
            double confidence = parseConfidence(confidenceStr);

            java.util.List<String> keywords = new java.util.ArrayList<>();
            if (keywordsStr != null && !keywordsStr.isEmpty()) {
                for (String keyword : keywordsStr.split(",")) {
                    keywords.add(keyword.trim());
                }
            }

            log.debug("LLM intent recognition result: intent={}, confidence={}", intentType, confidence);

            return IntentResult.builder()
                    .intentType(intentType)
                    .confidence(confidence)
                    .keywords(keywords)
                    .originalQuery(query)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse LLM intent response: {}", response, e);
            return null;
        }
    }

    private String extractField(String response, String prefix) {
        int start = response.indexOf(prefix);
        if (start == -1) {
            return null;
        }
        int end = response.indexOf("\n", start);
        if (end == -1) {
            end = response.length();
        }
        return response.substring(start + prefix.length(), end).trim();
    }

    private IntentType parseIntentType(String intentStr) {
        if (intentStr == null || intentStr.isEmpty()) {
            return IntentType.UNKNOWN;
        }
        try {
            return IntentType.valueOf(intentStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return IntentType.UNKNOWN;
        }
    }

    private double parseConfidence(String confidenceStr) {
        if (confidenceStr == null || confidenceStr.isEmpty()) {
            return 0.5;
        }
        try {
            return Math.min(1.0, Math.max(0.0, Double.parseDouble(confidenceStr.trim())));
        } catch (NumberFormatException e) {
            return 0.5;
        }
    }
}