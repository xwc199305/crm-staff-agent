package com.example.staffagent.context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SummaryGenerator {

    public String generateSummary(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        StringBuilder history = new StringBuilder();
        for (ChatMessage message : messages) {
            history.append(message.getRole()).append(": ").append(message.getContent()).append("\n");
        }

        String summary = "Conversation summary: User and assistant had multi-turn conversation covering multiple topics.";
        log.debug("Generated conversation summary, message count={}", messages.size());
        return summary;
    }

    public int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / 2;
    }

    public int countTokens(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        return messages.stream().mapToInt(m -> countTokens(m.getContent())).sum();
    }
}
