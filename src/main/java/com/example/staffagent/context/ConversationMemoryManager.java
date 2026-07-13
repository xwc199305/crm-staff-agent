package com.example.staffagent.context;

import com.example.staffagent.vector.VectorService;
import com.example.staffagent.vector.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationMemoryManager {

    private final VectorStore vectorStore;
    private final VectorService vectorService;
    private final SummaryGenerator summaryGenerator;

    private final Map<String, ConversationContext> contextMap = new ConcurrentHashMap<>();

    @Value("${memory.max-history-count:50}")
    private int maxHistoryCount;

    @Value("${memory.max-tokens:8000}")
    private int maxTokens;

    @Value("${memory.top-k:5}")
    private int topK;

    @Value("${memory.similarity-threshold:0.5}")
    private float similarityThreshold;

    @Value("${memory.summary-enabled:true}")
    private boolean summaryEnabled;

    @Value("${conversation.timeout-minutes:30}")
    private int timeoutMinutes;

    public void addMessage(String sessionId, String role, String content) {
        ConversationContext context = getContext(sessionId);

        float[] vector = vectorService.embed(content);
        int tokenCount = summaryGenerator.countTokens(content);

        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .timestamp(LocalDateTime.now())
                .vector(vector)
                .tokenCount(tokenCount)
                .build();

        context.addMessage(message);
        context.setTokenCount(context.getTokenCount() + tokenCount);
        context.setLastActiveTime(LocalDateTime.now());

        vectorStore.insert(List.of(message));

        if (summaryEnabled && context.getTokenCount() > maxTokens) {
            generateSummary(sessionId);
        }

        log.debug("Added message to Memory, sessionId={}, role={}, contentLength={}", sessionId, role, content.length());
    }

    public List<ChatMessage> retrieveRelevantMessages(String sessionId, String query) {
        float[] queryVector = vectorService.embed(query);
        List<ChatMessage> relevant = vectorStore.search(sessionId, queryVector, topK, similarityThreshold);

        log.debug("Retrieved {} relevant messages from vector database", relevant.size());
        return relevant;
    }

    public String buildContext(String sessionId, String query) {
        ConversationContext context = getContext(sessionId);
        List<ChatMessage> relevant = retrieveRelevantMessages(sessionId, query);
        List<ChatMessage> recent = getRecentMessages(sessionId, 6);

        Set<String> messageIds = new HashSet<>();
        StringBuilder contextBuilder = new StringBuilder();

        if (context.getSummary() != null && !context.getSummary().isEmpty()) {
            contextBuilder.append("[Conversation Summary]: ").append(context.getSummary()).append("\n\n");

        contextBuilder.append("[Recent Conversation History]:\n");
        for (ChatMessage message : recent) {
            contextBuilder.append(message.getRole()).append(": ").append(message.getContent()).append("\n");
            messageIds.add(message.getId());
        }
        contextBuilder.append("\n");

        contextBuilder.append("[Semantically Relevant Messages]:\n");
        for (ChatMessage message : relevant) {
            if (!messageIds.contains(message.getId())) {
                contextBuilder.append(message.getRole()).append(": ").append(message.getContent()).append("\n");
                messageIds.add(message.getId());
            }
        }

        contextBuilder.append("\nUser's Current Question: ").append(query);

        return contextBuilder.toString();
    }

    private List<ChatMessage> getRecentMessages(String sessionId, int limit) {
        ConversationContext context = getContext(sessionId);
        List<ChatMessage> messages = context.getMessages();
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        int start = Math.max(0, messages.size() - limit);
        return messages.subList(start, messages.size());
    }

    public String generateSummary(String sessionId) {
        ConversationContext context = getContext(sessionId);
        if (context.getMessages() == null || context.getMessages().isEmpty()) {
            return "";
        }

        String summary = summaryGenerator.generateSummary(context.getMessages());
        context.setSummary(summary);

        log.info("Generated summary for session {}", sessionId);
        return summary;
    }

    public void clearMemory(String sessionId) {
        vectorStore.deleteBySessionId(sessionId);
        contextMap.remove(sessionId);
        log.info("Cleared Memory for session {}", sessionId);
    }

    public MemoryStats getMemoryStats(String sessionId) {
        ConversationContext context = getContext(sessionId);
        int messageCount = context.getMessages() != null ? context.getMessages().size() : 0;
        int vectorCount = vectorStore.countBySessionId(sessionId);

        return MemoryStats.builder()
                .sessionId(sessionId)
                .messageCount(messageCount)
                .tokenCount(context.getTokenCount())
                .hasSummary(context.getSummary() != null && !context.getSummary().isEmpty())
                .vectorCount(vectorCount)
                .build();
    }

    private ConversationContext getContext(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = generateSessionId();
        }

        ConversationContext context = contextMap.get(sessionId);
        if (context == null) {
            context = ConversationContext.builder()
                    .sessionId(sessionId)
                    .lastActiveTime(LocalDateTime.now())
                    .turnCount(0)
                    .tokenCount(0)
                    .messages(new ArrayList<>())
                    .build();
            contextMap.put(sessionId, context);
        } else if (context.isExpired(timeoutMinutes)) {
            clearMemory(sessionId);
            context = ConversationContext.builder()
                    .sessionId(sessionId)
                    .lastActiveTime(LocalDateTime.now())
                    .turnCount(0)
                    .tokenCount(0)
                    .messages(new ArrayList<>())
                    .build();
            contextMap.put(sessionId, context);
        }

        return context;
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    @Scheduled(fixedDelayString = "${conversation.cleanup-interval-ms:300000}")
    public void cleanExpired() {
        int beforeSize = contextMap.size();
        contextMap.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired(timeoutMinutes);
            if (expired) {
                vectorStore.deleteBySessionId(entry.getKey());
            }
            return expired;
        });
        int afterSize = contextMap.size();
        if (beforeSize != afterSize) {
            log.info("Cleaned {} expired conversation contexts", beforeSize - afterSize);
        }
    }
}
