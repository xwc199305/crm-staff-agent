package com.example.staffagent.vector;

import com.example.staffagent.context.ChatMessage;

import java.util.List;

public interface VectorStore {
    void init();
    void insert(List<ChatMessage> messages);
    List<ChatMessage> search(String sessionId, float[] queryVector, int topK, float threshold);
    void deleteBySessionId(String sessionId);
    int countBySessionId(String sessionId);
    boolean isConnected();
}
