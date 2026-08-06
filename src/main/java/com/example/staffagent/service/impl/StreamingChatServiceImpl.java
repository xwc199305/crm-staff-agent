package com.example.staffagent.service.impl;

import com.example.staffagent.context.ConversationContextHolder;
import com.example.staffagent.dify.dto.DifyResponse;
import com.example.staffagent.service.StreamingChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
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
public class StreamingChatServiceImpl implements StreamingChatService {

    @Value("${agent.api-key:}")
    private String apiKey;

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
        log.info("Starting simple stream chat, prompt length={}", prompt.length());
        return doStream(prompt);
    }

    @Override
    public Flux<String> streamChat(String query, List<DifyResponse.Record> records) {
        if (records == null || records.isEmpty()) {
            log.debug("No records provided for streaming RAG, falling back to simple stream");
            return doStream(query);
        }

        if (promptTemplate == null || promptTemplate.isEmpty()) {
            log.warn("RAG prompt template not configured, falling back to simple stream");
            return doStream(query);
        }

        try {
            List<DifyResponse.Record> limitedRecords = records.stream()
                    .limit(maxRecords)
                    .collect(Collectors.toList());

            String context = recordsToJson(limitedRecords);

            String historyContext = ConversationContextHolder.getContext();
            if (historyContext != null && !historyContext.isEmpty()) {
                context = historyContext + "\n\n[Knowledge Base Context]:\n" + context;
                log.debug("Added history context to streaming RAG, history length={}", historyContext.length());
            }

            String prompt = buildPrompt(query, context);
            log.debug("Generated streaming RAG prompt, context length={}", context.length());

            return doStream(prompt);
        } catch (Exception e) {
            log.error("Streaming RAG prompt build failed", e);
            return Flux.just("Failed to generate response.");
        }
    }

    private Flux<String> doStream(String prompt) {
        ReActAgent agent = createAgent();
        Msg msg = Msg.builder()
                .textContent(prompt)
                .build();

        StreamOptions options = StreamOptions.builder()
                .incremental(true)
                .includeReasoningChunk(false)
                .build();

        log.info("Calling ReActAgent.stream()");

        return agent.stream(msg, options)
                .filter(event -> event.getType() == EventType.AGENT_RESULT)
                .map(Event::getMessage)
                .map(Msg::getTextContent)
                .filter(text -> text != null && !text.isEmpty())
                .doOnNext(text -> log.debug("Streamed chunk: {}", text.length() > 50 ? text.substring(0, 50) + "..." : text))
                .doOnComplete(() -> log.info("Stream completed"))
                .doOnError(e -> log.error("Stream error", e));
    }

    private ReActAgent createAgent() {
        String actualApiKey = apiKey;
        if (actualApiKey == null || actualApiKey.isEmpty()) {
            actualApiKey = System.getenv("DASHSCOPE_API_KEY");
        }

        if (actualApiKey == null || actualApiKey.isEmpty()) {
            throw new IllegalStateException("API key not configured for streaming chat");
        }

        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(actualApiKey)
                .modelName("qwen-max")
                .build();

        return ReActAgent.builder()
                .name("Streaming-Assistant")
                .sysPrompt("You are a professional knowledge QA assistant. Answer user questions based on the provided context.")
                .model(model)
                .build();
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
