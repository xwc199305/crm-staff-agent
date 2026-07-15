package com.example.staffagent.context;

import com.example.staffagent.intent.IntentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext {
    private String userId;
    private String sessionId;
    private IntentType lastIntent;
    private String lastQuery;
    private String lastReply;
    private LocalDateTime lastActiveTime;
    private int turnCount;
    private List<ChatMessage> messages;
    private String summary;
    private int tokenCount;

    public void update(IntentType intent, String query, String reply) {
        this.lastIntent = intent;
        this.lastQuery = query;
        this.lastReply = reply;
        this.lastActiveTime = LocalDateTime.now();
        this.turnCount++;
    }

    public boolean isExpired(int timeoutMinutes) {
        return lastActiveTime.plusMinutes(timeoutMinutes).isBefore(LocalDateTime.now());
    }

    public void addMessage(ChatMessage message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        this.lastActiveTime = LocalDateTime.now();
    }

    public void clearMessages() {
        if (messages != null) {
            messages.clear();
        }
    }
}