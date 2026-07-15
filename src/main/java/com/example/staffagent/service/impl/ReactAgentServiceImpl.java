package com.example.staffagent.service.impl;

import com.example.staffagent.context.ConversationContextHolder;
import com.example.staffagent.context.ConversationMemoryManager;
import com.example.staffagent.dto.ChatResponse;
import com.example.staffagent.dto.IntentResult;
import com.example.staffagent.handler.impl.IntentHandlerFactory;
import com.example.staffagent.intent.IntentRecognizer;
import com.example.staffagent.intent.IntentType;
import com.example.staffagent.service.ReactAgentService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReactAgentServiceImpl implements ReactAgentService {

    private static final String DEFAULT_REPLY = "Sorry, we cannot understand your question. Please rephrase.";

	private ReActAgent agent;

    private final IntentRecognizer intentRecognizer;
    private final IntentHandlerFactory handlerFactory;
    private final ConversationMemoryManager memoryManager;

    @Value("${agent.name:React Assistant}")
    private String name;

    @Value("${agent.api-key:}")
    private String apiKey;

    @PostConstruct
    public void init() {
        log.info("ReactAgentService initialized, name={}", name);
    }

    private ReActAgent getAgent() {
        if (agent == null) {
            String actualApiKey = apiKey;
            if (actualApiKey == null || actualApiKey.isEmpty()) {
                actualApiKey = System.getenv("DASHSCOPE_API_KEY");
            }

            if (actualApiKey == null || actualApiKey.isEmpty()) {
                throw new IllegalStateException("API key not configured. Please configure agent.api-key in application.properties or set environment variable DASHSCOPE_API_KEY");
            }

            DashScopeChatModel model = DashScopeChatModel.builder()
                    .apiKey(actualApiKey)
                    .modelName("qwen-max")
                    .build();

            agent = ReActAgent.builder()
                    .name(name)
                    .sysPrompt("You are a helpful AI assistant.")
                    .model(model)
                    .build();
        }
        return agent;
    }

    @Override
    public String call(String userInput) {
        log.info("Received user input: {}", userInput);
        try {
            Msg msg = Msg.builder()
                    .textContent(userInput)
                    .build();

            Mono<Msg> responseMono = getAgent().call(msg);
            Msg response = responseMono.block();

            String result = response != null ? response.getTextContent() : "Sorry, I cannot generate a response.";
            log.info("Agent response: {}", result);
            return result;
        } catch (IllegalStateException e) {
            log.warn("API key not configured: {}", e.getMessage());
            return e.getMessage();
        } catch (Exception e) {
            log.error("Error calling Agent", e);
            throw new RuntimeException("Agent invocation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ChatResponse chatWithIntent(String userId, String userInput, String sessionId) {
        log.info("Chatting with intent recognition, userId={}, sessionId={}, input={}", userId, sessionId, userInput);

        String context = memoryManager.buildContext(userId, sessionId, userInput);
        log.info("Context built, length={}, contains mem0 memories={}", context.length(), context.contains("mem0"));

        ConversationContextHolder.setContext(context);

        try {
            IntentResult intentResult = intentRecognizer.recognize(userInput);
            IntentType intentType = intentResult.getIntentType();

            String reply;
            if (intentType == IntentType.UNKNOWN) {
                reply = DEFAULT_REPLY;
                log.info("Intent recognized as UNKNOWN, returning fallback reply");
            } else if (handlerFactory.hasHandler(intentType)) {
                reply = handlerFactory.handleWithToolCall(userInput, intentType);
                log.info("Intent handler responded: {} -> {}", intentType, reply);
            } else {
                reply = DEFAULT_REPLY;
                log.info("No handler found for intent: {}, returning fallback reply", intentType);
            }

            memoryManager.addMessagePair(userId, sessionId, userInput, reply);

            return ChatResponse.builder()
                    .reply(reply)
                    .intentType(intentType)
                    .intentDescription(intentType.getDescription())
                    .intentConfidence(intentResult.getConfidence())
                    .build();
        } finally {
            ConversationContextHolder.clearContext();
        }
    }

    @Override
    public IntentResult recognizeIntent(String userInput) {
        return intentRecognizer.recognize(userInput);
    }
}