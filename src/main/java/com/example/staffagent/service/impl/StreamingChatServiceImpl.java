package com.example.staffagent.service.impl;

import com.example.staffagent.dify.dto.DifyResponse;
import com.example.staffagent.service.StreamingChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingChatServiceImpl implements StreamingChatService {

    private final DashScopeStreamClient dashScopeStreamClient;

    @Value("${rag.prompt-template:}")
    private String promptTemplate;

    @Value("${rag.max-records:3}")
    private Integer maxRecords;

    @Value("${rag.max-content-length:2000}")
    private Integer maxContentLength;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public Flux<String> streamChat(String prompt) {
        log.info("Starting simple stream chat via DashScope SSE, prompt length={}", prompt.length());
        return dashScopeStreamClient.streamChat(prompt, null);
    }

    @Override
    public Flux<String> streamChat(String query, List<DifyResponse.Record> records) {
        if (records == null || records.isEmpty()) {
            log.debug("No records provided for streaming RAG, falling back to simple stream");
            return dashScopeStreamClient.streamChat(query, null);
        }

        if (promptTemplate == null || promptTemplate.isEmpty()) {
            log.warn("RAG prompt template not configured, falling back to simple stream");
            return dashScopeStreamClient.streamChat(query, null);
        }

        try {
            List<DifyResponse.Record> limitedRecords = records.stream()
                    .limit(maxRecords)
                    .collect(Collectors.toList());

            String context = recordsToJson(limitedRecords);

            String prompt = buildPrompt(query, context);
            log.info("Generating streaming RAG prompt, context length={}", context.length());

            return dashScopeStreamClient.streamChat(prompt, null);
        } catch (Exception e) {
            log.error("Streaming RAG prompt build failed", e);
            return Flux.just("Failed to generate response.");
        }
    }

    private String recordsToJson(List<DifyResponse.Record> records) throws JsonProcessingException {
        List<Map<String, Object>> jsonRecords = new ArrayList<>();

        for (DifyResponse.Record record : records) {
            Map<String, Object> jsonRecord = new HashMap<>();

            if (record.getSegment() != null) {
                String content = record.getSegment().getContent();
                if (content != null && content.length() > maxContentLength) {
                    content = content.substring(0, maxContentLength) + "...";
                }
                jsonRecord.put("content", content);

                if (record.getSegment().getDocument() != null) {
                    jsonRecord.put("document_name", record.getSegment().getDocument().getName());
                }

                if (record.getSegment().getKeywords() != null) {
                    jsonRecord.put("keywords", record.getSegment().getKeywords());
                }
            }

            if (record.getScore() != null) {
                jsonRecord.put("score", record.getScore());
            }

            jsonRecords.add(jsonRecord);
        }

        return objectMapper.writeValueAsString(jsonRecords);
    }

    private String buildPrompt(String query, String context) {
        return promptTemplate
                .replace("{{context}}", context)
                .replace("{{query}}", query);
    }
}
