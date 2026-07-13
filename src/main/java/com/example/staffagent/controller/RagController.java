package com.example.staffagent.controller;

import com.example.staffagent.dto.ApiResponse;
import com.example.staffagent.dify.DifyKnowledgeBaseService;
import com.example.staffagent.dify.dto.DifyResponse;
import com.example.staffagent.dify.dto.KnowledgeBaseInfo;
import com.example.staffagent.intent.IntentType;
import com.example.staffagent.service.RagService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Slf4j
public class RagController {

    private final DifyKnowledgeBaseService knowledgeBaseService;
    private final RagService ragService;

    @GetMapping("/knowledge-bases")
    public ApiResponse<List<KnowledgeBaseInfo>> getKnowledgeBaseList() {
        log.info("Getting knowledge base list");
        List<KnowledgeBaseInfo> list = knowledgeBaseService.getKnowledgeBaseList();
        return ApiResponse.success(list);
    }

    @PostMapping("/refresh-knowledge-bases")
    public ApiResponse<String> refreshKnowledgeBaseList() {
        log.info("Refreshing knowledge base list");
        knowledgeBaseService.refreshKnowledgeBaseList();
        return ApiResponse.success("Knowledge base list refreshed");
    }

    @PostMapping("/retrieve")
    public ApiResponse<RetrieveResult> retrieve(@RequestBody RetrieveRequest request) {
        log.info("Retrieving records from knowledge base, query: {}", request.getQuery());

        List<DifyResponse.Record> records = knowledgeBaseService.retrieveRecords(request.getQuery());

        RetrieveResult result = RetrieveResult.builder()
                .query(request.getQuery())
                .recordCount(records.size())
                .records(records)
                .build();

        return ApiResponse.success(result);
    }

    @PostMapping("/retrieve-by-intent")
    public ApiResponse<RetrieveResult> retrieveByIntent(@RequestBody RetrieveByIntentRequest request) {
        log.info("Retrieving records by intent, intent: {}, query: {}", request.getIntentType(), request.getQuery());

        IntentType intentType;
        try {
            intentType = IntentType.valueOf(request.getIntentType().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("Invalid intent type: " + request.getIntentType());
        }

        String datasetId = knowledgeBaseService.findDatasetId(request.getQuery(), intentType);
        log.info("Matched dataset: {}", datasetId);

        List<DifyResponse.Record> records = knowledgeBaseService.retrieveRecordsByIntent(request.getQuery(), intentType);

        RetrieveResult result = RetrieveResult.builder()
                .query(request.getQuery())
                .intentType(intentType.name())
                .datasetId(datasetId)
                .recordCount(records.size())
                .records(records)
                .build();

        return ApiResponse.success(result);
    }

    @PostMapping("/generate")
    public ApiResponse<GenerateResult> generate(@RequestBody GenerateRequest request) {
        log.info("Generating RAG response, query: {}", request.getQuery());

        List<DifyResponse.Record> records;

        if (request.getIntentType() != null && !request.getIntentType().isEmpty()) {
            IntentType intentType;
            try {
                intentType = IntentType.valueOf(request.getIntentType().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ApiResponse.error("Invalid intent type: " + request.getIntentType());
            }
            records = knowledgeBaseService.retrieveRecordsByIntent(request.getQuery(), intentType);
        } else {
            records = knowledgeBaseService.retrieveRecords(request.getQuery());
        }

        String answer;
        if (records.isEmpty()) {
            answer = "No relevant information available";
        } else {
            answer = ragService.generate(request.getQuery(), records);
        }

        GenerateResult result = GenerateResult.builder()
                .query(request.getQuery())
                .recordCount(records.size())
                .answer(answer)
                .build();

        return ApiResponse.success(result);
    }

    @Data
    public static class RetrieveRequest {
        private String query;
    }

    @Data
    public static class RetrieveByIntentRequest {
        private String query;
        private String intentType;
    }

    @Data
    public static class GenerateRequest {
        private String query;
        private String intentType;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RetrieveResult {
        private String query;
        private String intentType;
        private String datasetId;
        private int recordCount;
        private List<DifyResponse.Record> records;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GenerateResult {
        private String query;
        private int recordCount;
        private String answer;
    }
}