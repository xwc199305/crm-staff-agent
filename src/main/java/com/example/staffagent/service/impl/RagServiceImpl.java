package com.example.staffagent.service.impl;

import com.example.staffagent.dify.DifyKnowledgeBaseService;
import com.example.staffagent.dify.dto.DifyResponse;
import com.example.staffagent.service.RagService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final DifyKnowledgeBaseService knowledgeBaseService;

    @Value("${rag.prompt-template:}")
    private String promptTemplate;

    @Value("${rag.max-records:3}")
    private Integer maxRecords;

    @Value("${rag.max-content-length:2000}")
    private Integer maxContentLength;

    @Value("${agent.api-key:}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public String generate(String query, List<DifyResponse.Record> records) {
        if (records == null || records.isEmpty()) {
            log.debug("No records provided for RAG generation");
            return "";
        }

        if (promptTemplate == null || promptTemplate.isEmpty()) {
            log.warn("RAG prompt template not configured");
            return "";
        }

        try {
            List<DifyResponse.Record> limitedRecords = records.stream()
                    .limit(maxRecords)
                    .collect(Collectors.toList());

            String context = recordsToJson(limitedRecords);
            String prompt = buildPrompt(query, context);

            log.debug("Generated RAG prompt, context length={}", context.length());

            return callLlm(prompt);
        } catch (Exception e) {
            log.error("RAG generation failed", e);
            return "";
        }
    }

    @Override
    public String generateWithQuery(String query) {
        List<DifyResponse.Record> records = knowledgeBaseService.retrieveRecords(query);
        if (records.isEmpty()) {
            log.debug("No records found for query: {}", query);
            return "";
        }
        return generate(query, records);
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

    private String callLlm(String prompt) {
        String actualApiKey = apiKey;
        if (actualApiKey == null || actualApiKey.isEmpty()) {
            actualApiKey = System.getenv("DASHSCOPE_API_KEY");
        }

        if (actualApiKey == null || actualApiKey.isEmpty()) {
            log.warn("API key not configured for RAG generation");
            return "";
        }

        try {
            DashScopeChatModel model = DashScopeChatModel.builder()
                    .apiKey(actualApiKey)
                    .modelName("qwen-max")
                    .build();

            ReActAgent agent = ReActAgent.builder()
                    .name("RAG-Assistant")
                    .sysPrompt("You are a professional knowledge QA assistant. Answer user questions based on the provided context.")
                    .model(model)
                    .build();

            Msg msg = Msg.builder()
                    .textContent(prompt)
                    .build();

            Mono<Msg> responseMono = agent.call(msg);
            Msg response = responseMono.block();

            String result = response != null ? response.getTextContent() : "";
            log.info("RAG LLM response: {}", result.length() > 100 ? result.substring(0, 100) + "..." : result);
            return result;
        } catch (Exception e) {
            log.error("LLM call failed for RAG", e);
            return "";
        }
    }
}