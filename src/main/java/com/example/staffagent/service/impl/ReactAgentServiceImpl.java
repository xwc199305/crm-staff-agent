package com.example.staffagent.service.impl;

import com.example.staffagent.agent.StaffAgentTools;
import com.example.staffagent.dto.ChatResponse;
import com.example.staffagent.dto.IntentResult;
import com.example.staffagent.graph.IntentWorkflow;
import com.example.staffagent.intent.IntentRecognizer;
import com.example.staffagent.service.ReactAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReactAgentServiceImpl implements ReactAgentService {

    private final IntentRecognizer intentRecognizer;
    private final IntentWorkflow intentWorkflow;
    private final ChatClient.Builder chatClientBuilder;
    private final StaffAgentTools staffAgentTools;

    @Value("${agent.name:React Assistant}")
    private String name;

    @Value("${agent.system-prompt:You are a helpful assistant.}")
    private String systemPrompt;

    @Override
    public String call(String userInput) {
        log.info("Received user input: {}", userInput);
        try {
            String result = chatClientBuilder.build().prompt()
                    .system(systemPrompt)
                    .user(userInput)
                    .tools(staffAgentTools)
                    .call()
                    .content();
            log.info("Agent response: {}", result);
            return result;
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

        return intentWorkflow.execute(userId, userInput, sessionId);
    }

    @Override
    public IntentResult recognizeIntent(String userInput) {
        return intentRecognizer.recognize(userInput);
    }
}
