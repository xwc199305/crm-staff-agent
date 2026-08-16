package com.example.staffagent.dify.impl;

import com.example.staffagent.config.KnowledgeBaseProperties;
import com.example.staffagent.dify.KnowledgeBaseMatcher;
import com.example.staffagent.dify.dto.KnowledgeBaseInfo;
import com.example.staffagent.intent.IntentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class LLMKnowledgeBaseMatcher implements KnowledgeBaseMatcher {

    @Value("${kb.matcher.llm.enabled:true}")
    private boolean enabled;

    @Value("${kb.matcher.llm.prompt-template:}")
    private String promptTemplate;

    private final KnowledgeBaseProperties kbProperties;
    private final ChatClient.Builder chatClientBuilder;

    private static final String DEFAULT_RESULT = "DEFAULT";
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String match(IntentType intentType, String query, List<KnowledgeBaseInfo> knowledgeBaseList) {
        if (!enabled) {
            log.debug("LLM knowledge base matcher is disabled");
            return DEFAULT_RESULT;
        }

        if (knowledgeBaseList == null || knowledgeBaseList.isEmpty()) {
            log.debug("No knowledge base list provided for LLM matching");
            return DEFAULT_RESULT;
        }

        if (promptTemplate == null || promptTemplate.isEmpty()) {
            log.warn("LLM matcher prompt template not configured");
            return DEFAULT_RESULT;
        }

        try {
            String prompt = buildPrompt(intentType, query, knowledgeBaseList);
            log.debug("Built LLM matching prompt: {}", prompt.length() > 500 ? prompt.substring(0, 500) + "..." : prompt);

            String response = callLlm(prompt);
            log.debug("LLM matching response: {}", response);

            String datasetId = parseResponse(response);
            log.info("LLM matched intent {} to dataset {}", intentType, datasetId);

            return datasetId;
        } catch (Exception e) {
            log.warn("LLM matching failed: {}", e.getMessage());
            log.debug("LLM matching exception", e);
            return DEFAULT_RESULT;
        }
    }

    private String buildPrompt(IntentType intentType, String query, List<KnowledgeBaseInfo> knowledgeBaseList) {
        String knowledgeBaseListStr = formatKnowledgeBaseList(knowledgeBaseList);

        return promptTemplate
                .replace("{intentType}", intentType.name())
                .replace("{intentDescription}", intentType.getDescription())
                .replace("{query}", query != null ? query : "")
                .replace("{knowledgeBaseList}", knowledgeBaseListStr);
    }

    private String formatKnowledgeBaseList(List<KnowledgeBaseInfo> kbList) {
        StringBuilder sb = new StringBuilder();
        for (KnowledgeBaseInfo kb : kbList) {
            sb.append("- ID: ").append(kb.getId())
                    .append(" | Name: ").append(kb.getName())
                    .append(" | Description: ").append(kb.getDescription() != null ? kb.getDescription() : "None")
                    .append(" | Tags: ").append(kb.getTags() != null ? kb.getTags().stream().map(KnowledgeBaseInfo.Tag::getName).collect(java.util.stream.Collectors.joining(",")) : "None")
                    .append("\n");
        }
        return sb.toString();
    }

    private String callLlm(String prompt) {
        String response = chatClientBuilder.build().prompt()
                .system("You are a knowledge base classification expert. Select the most suitable knowledge base based on user intent and knowledge base information.")
                .user(prompt)
                .call()
                .content();
        return response == null ? "" : response;
    }

    private String parseResponse(String response) {
        if (response == null || response.isEmpty()) {
            return DEFAULT_RESULT;
        }

        List<String> prefixes = kbProperties.getResponsePrefixes();
        
        for (String prefix : prefixes) {
            int start = response.indexOf(prefix);
            if (start != -1) {
                int contentStart = start + prefix.length();
                int end = response.indexOf("\n", contentStart);
                if (end == -1) {
                    end = response.length();
                }
                String datasetId = response.substring(contentStart, end).trim();
                return datasetId.isEmpty() ? DEFAULT_RESULT : datasetId;
            }
        }

        Matcher matcher = UUID_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group();
        }

        return DEFAULT_RESULT;
    }
}
