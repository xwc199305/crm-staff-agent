package com.example.staffagent.context;

import com.example.staffagent.intent.IntentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ConversationContextManager {

    private final Map<String, ConversationContext> contextMap = new ConcurrentHashMap<>();

    @Value("${conversation.timeout-minutes:30}")
    private int timeoutMinutes;

    public ConversationContext getContext(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = generateSessionId();
        }

        ConversationContext context = contextMap.get(sessionId);
        if (context == null) {
            context = ConversationContext.builder()
                    .sessionId(sessionId)
                    .lastActiveTime(LocalDateTime.now())
                    .turnCount(0)
                    .build();
            contextMap.put(sessionId, context);
            log.debug("Created new conversation context: {}", sessionId);
        } else if (context.isExpired(timeoutMinutes)) {
            log.debug("Conversation context expired, creating new one: {}", sessionId);
            context = ConversationContext.builder()
                    .sessionId(sessionId)
                    .lastActiveTime(LocalDateTime.now())
                    .turnCount(0)
                    .build();
            contextMap.put(sessionId, context);
        }

        return context;
    }

    public void updateContext(String sessionId, IntentType intent, String query, String reply) {
        ConversationContext context = getContext(sessionId);
        context.update(intent, query, reply);
        log.debug("Updated conversation context: {}, intent: {}", sessionId, intent);
    }

    @Scheduled(fixedDelayString = "${conversation.cleanup-interval-ms:300000}")
    public void cleanExpired() {
        int beforeSize = contextMap.size();
        contextMap.entrySet().removeIf(entry -> entry.getValue().isExpired(timeoutMinutes));
        int afterSize = contextMap.size();
        if (beforeSize != afterSize) {
            log.info("Cleaned {} expired conversation contexts", beforeSize - afterSize);
        }
    }

    public String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public int getActiveContextCount() {
        return contextMap.size();
    }
}