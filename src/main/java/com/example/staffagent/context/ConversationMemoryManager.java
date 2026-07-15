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
    private final Mem0Client mem0Client;

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

    public void addMessage(String userId, String sessionId, String role, String content) {
        ConversationContext context = getContext(userId, sessionId);

        float[] vector = vectorService.embed(content);
        int tokenCount = summaryGenerator.countTokens(content);

        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
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

        if (mem0Client.isEnabled()) {
            mem0Client.addMemory(userId, content, role);
            log.debug("Added message to mem0 server, userId={}, sessionId={}, role={}, contentLength={}", userId, sessionId, role, content.length());
        } else {
            vectorStore.insert(List.of(message));
            log.debug("Added message to Qdrant vector store, userId={}, sessionId={}, role={}, contentLength={}", userId, sessionId, role, content.length());
        }

        if (summaryEnabled && context.getTokenCount() > maxTokens) {
            generateSummary(userId, sessionId);
        }
    }

    public void addMessagePair(String userId, String sessionId, String userMessage, String assistantReply) {
        if (mem0Client.isEnabled()) {
            addMessageInternal(userId, sessionId, "user", userMessage);
            addMessageInternal(userId, sessionId, "assistant", assistantReply);
            mem0Client.addMessageMemory(userId, userMessage, assistantReply);
        } else {
            addMessage(userId, sessionId, "user", userMessage);
            addMessage(userId, sessionId, "assistant", assistantReply);
        }
    }

    private void addMessageInternal(String userId, String sessionId, String role, String content) {
        ConversationContext context = getContext(userId, sessionId);

        float[] vector = vectorService.embed(content);
        int tokenCount = summaryGenerator.countTokens(content);

        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
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

        log.debug("Added message internally, userId={}, sessionId={}, role={}, contentLength={}", userId, sessionId, role, content.length());

        if (summaryEnabled && context.getTokenCount() > maxTokens) {
            generateSummary(userId, sessionId);
        }
    }

    public List<ChatMessage> retrieveRelevantMessages(String userId, String sessionId, String query) {
        if (mem0Client.isEnabled()) {
            List<ChatMessage> relevant = mem0Client.searchMemories(userId, query);
            log.debug("Retrieved {} relevant messages from mem0 server for userId {}", relevant.size(), userId);
            return relevant;
        }

        float[] queryVector = vectorService.embed(query);
        List<ChatMessage> relevant = vectorStore.search(sessionId, queryVector, topK, similarityThreshold);

        log.debug("Retrieved {} relevant messages from Qdrant vector database", relevant.size());
        return relevant;
    }

    public List<ChatMessage> retrieveUserMemory(String userId, String query) {
        if (mem0Client.isEnabled()) {
            return mem0Client.searchMemories(userId, query);
        }

        float[] queryVector = vectorService.embed(query);
        List<ChatMessage> relevant = ((com.example.staffagent.vector.QdrantVectorStore) vectorStore)
                .searchByUserId(userId, queryVector, topK, similarityThreshold);

        log.debug("Retrieved {} relevant memory items for userId {}", relevant.size(), userId);
        return relevant;
    }

    public String buildContext(String userId, String sessionId, String query) {
        ConversationContext context = getContext(userId, sessionId);
        
        List<ChatMessage> relevant = retrieveRelevantMessages(userId, sessionId, query);
        List<ChatMessage> recent = getRecentMessages(userId, sessionId, 6);

        Set<String> messageIds = new HashSet<>();
        StringBuilder contextBuilder = new StringBuilder();

        if (context.getSummary() != null && !context.getSummary().isEmpty()) {
            contextBuilder.append("[Conversation Summary]: ").append(context.getSummary()).append("\n\n");
        }

        contextBuilder.append("[Recent Conversation History]:\n");
        for (ChatMessage message : recent) {
            contextBuilder.append(message.getRole()).append(": ").append(message.getContent()).append("\n");
            messageIds.add(message.getId());
        }
        contextBuilder.append("\n");

        if (mem0Client.isEnabled()) {
            List<ChatMessage> mem0Memories = mem0Client.getUserMemories(userId);
            if (!mem0Memories.isEmpty()) {
                contextBuilder.append("[User Long-term Memory (from mem0)]:\n");
                for (ChatMessage memory : mem0Memories) {
                    if (!messageIds.contains(memory.getId())) {
                        contextBuilder.append("Memory: ").append(memory.getContent()).append("\n");
                        messageIds.add(memory.getId());
                    }
                }
                contextBuilder.append("\n");
            }
        }

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

    private List<ChatMessage> getRecentMessages(String userId, String sessionId, int limit) {
        ConversationContext context = getContext(userId, sessionId);
        List<ChatMessage> messages = context.getMessages();
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        int start = Math.max(0, messages.size() - limit);
        return messages.subList(start, messages.size());
    }

    public String generateSummary(String userId, String sessionId) {
        ConversationContext context = getContext(userId, sessionId);
        if (context.getMessages() == null || context.getMessages().isEmpty()) {
            return "";
        }

        String summary = summaryGenerator.generateSummary(context.getMessages());
        context.setSummary(summary);

        log.info("Generated summary for userId {}, session {}", userId, sessionId);
        return summary;
    }

    public void clearMemory(String userId, String sessionId) {
        if (mem0Client.isEnabled()) {
            log.info("Clearing session memory skipped (mem0 manages user-level memory), userId={}, session={}", userId, sessionId);
        } else {
            vectorStore.deleteBySessionId(sessionId);
        }
        contextMap.remove(sessionId);
        log.info("Cleared Memory for userId {}, session {}", userId, sessionId);
    }

    public void clearUserMemory(String userId) {
        if (mem0Client.isEnabled()) {
            mem0Client.deleteUserMemories(userId);
            log.info("Cleared all Memory for userId {} via mem0 server", userId);
        } else {
            ((com.example.staffagent.vector.QdrantVectorStore) vectorStore).deleteByUserId(userId);
            log.info("Cleared all Memory for userId {} via Qdrant", userId);
        }
        contextMap.entrySet().removeIf(entry -> entry.getValue().getUserId().equals(userId));
    }

    public MemoryStats getMemoryStats(String userId, String sessionId) {
        ConversationContext context = getContext(userId, sessionId);
        int messageCount = context.getMessages() != null ? context.getMessages().size() : 0;
        
        int vectorCount;
        if (mem0Client.isEnabled()) {
            vectorCount = mem0Client.getUserMemories(userId).size();
        } else {
            vectorCount = vectorStore.countBySessionId(sessionId);
        }

        return MemoryStats.builder()
                .userId(userId)
                .sessionId(sessionId)
                .messageCount(messageCount)
                .tokenCount(context.getTokenCount())
                .hasSummary(context.getSummary() != null && !context.getSummary().isEmpty())
                .vectorCount(vectorCount)
                .build();
    }

    public int getUserMemoryCount(String userId) {
        if (mem0Client.isEnabled()) {
            return mem0Client.getUserMemories(userId).size();
        }
        return ((com.example.staffagent.vector.QdrantVectorStore) vectorStore).countByUserId(userId);
    }

    public boolean isMem0Enabled() {
        return mem0Client.isEnabled();
    }

    public boolean isMem0Connected() {
        return mem0Client.isConnected();
    }

    private ConversationContext getContext(String userId, String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = generateSessionId();
        }

        ConversationContext context = contextMap.get(sessionId);
        if (context == null) {
            context = ConversationContext.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .lastActiveTime(LocalDateTime.now())
                    .turnCount(0)
                    .tokenCount(0)
                    .messages(new ArrayList<>())
                    .build();
            contextMap.put(sessionId, context);
        } else if (!userId.equals(context.getUserId())) {
            clearMemory(context.getUserId(), sessionId);
            context = ConversationContext.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .lastActiveTime(LocalDateTime.now())
                    .turnCount(0)
                    .tokenCount(0)
                    .messages(new ArrayList<>())
                    .build();
            contextMap.put(sessionId, context);
        } else if (context.isExpired(timeoutMinutes)) {
            clearMemory(userId, sessionId);
            context = ConversationContext.builder()
                    .userId(userId)
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
                if (!mem0Client.isEnabled()) {
                    vectorStore.deleteBySessionId(entry.getKey());
                }
            }
            return expired;
        });
        int afterSize = contextMap.size();
        if (beforeSize != afterSize) {
            log.info("Cleaned {} expired conversation contexts", beforeSize - afterSize);
        }
    }
}