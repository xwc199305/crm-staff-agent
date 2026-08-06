package com.example.staffagent.dify.impl;

import com.example.staffagent.config.KnowledgeBaseProperties;
import com.example.staffagent.dify.DifyClient;
import com.example.staffagent.dify.DifyKnowledgeBaseService;
import com.example.staffagent.dify.KnowledgeBaseMatcher;
import com.example.staffagent.dify.dto.DifyResponse;
import com.example.staffagent.dify.dto.KnowledgeBaseInfo;
import com.example.staffagent.dify.dto.KnowledgeBaseListResponse;
import com.example.staffagent.intent.IntentType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DifyKnowledgeBaseServiceImpl implements DifyKnowledgeBaseService {

    private final DifyClient difyClient;
    private final KnowledgeBaseMatcher llmKnowledgeBaseMatcher;
    private final KnowledgeBaseProperties kbProperties;

    private List<KnowledgeBaseInfo> cachedKnowledgeBaseList = Collections.emptyList();

    @PostConstruct
    public void init() {
        refreshKnowledgeBaseList();
    }

    @Override
    public Optional<String> query(String query) {
        if (!isEnabled()) {
            log.debug("Dify knowledge base is not enabled");
            return Optional.empty();
        }

        log.info("Querying Dify knowledge base: {}", query);

        DifyResponse response = difyClient.retrieve(query);

        if (response.isSuccess() && response.getRecords() != null && !response.getRecords().isEmpty()) {
            String answer = response.getRecords().stream()
                    .filter(record -> record.getSegment() != null && record.getSegment().getContent() != null)
                    .map(record -> record.getSegment().getContent())
                    .collect(Collectors.joining("\n\n"));

            if (!answer.isEmpty()) {
                log.info("Dify knowledge base returned {} records", response.getRecords().size());
                return Optional.of(answer.trim());
            }
        }

        log.warn("Dify knowledge base query returned no results or failed: {}", response.getError());
        return Optional.empty();
    }

    @Override
    public List<DifyResponse.Record> retrieveRecords(String query) {
        if (!isEnabled()) {
            log.debug("Dify knowledge base is not enabled");
            return Collections.emptyList();
        }

        log.info("Retrieving records from Dify knowledge base: {}", query);

        DifyResponse response = difyClient.retrieve(query);

        if (response.isSuccess() && response.getRecords() != null) {
            log.info("Dify knowledge base returned {} records", response.getRecords().size());
            return response.getRecords();
        }

        log.warn("Dify knowledge base retrieval failed: {}", response.getError());
        return Collections.emptyList();
    }

    @Override
    public List<DifyResponse.Record> retrieveRecordsByIntent(String query, IntentType intentType) {
        if (!isEnabled()) {
            log.debug("Dify knowledge base is not enabled");
            return Collections.emptyList();
        }

        String datasetId = findDatasetId(query, intentType);

        if (datasetId == null || datasetId.isEmpty()) {
            log.warn("No dataset ID available for intent: {}", intentType);
            return Collections.emptyList();
        }

        log.info("Retrieving records from Dify knowledge base for intent {} using dataset {}", intentType, datasetId);

        DifyResponse response = difyClient.retrieve(query, datasetId);

        if (response.isSuccess() && response.getRecords() != null) {
            log.info("Dify knowledge base returned {} records", response.getRecords().size());
            return response.getRecords();
        }

        log.warn("Dify knowledge base retrieval failed: {}", response.getError());
        return Collections.emptyList();
    }

    @Override
    public String findDatasetId(String query, IntentType intentType) {
        String datasetId = llmKnowledgeBaseMatcher.match(intentType, query, cachedKnowledgeBaseList);
        if (datasetId != null && !datasetId.isEmpty() && !"DEFAULT".equals(datasetId)) {
            log.info("LLM matched intent {} to dataset {}", intentType, datasetId);
            return datasetId;
        }

        log.debug("LLM match returned DEFAULT or empty, trying keyword match");
        datasetId = autoMatchKnowledgeBase(intentType);
        if (datasetId != null && !datasetId.isEmpty()) {
            return datasetId;
        }

        datasetId = difyClient.getDefaultDatasetId();
        if (datasetId != null && !datasetId.isEmpty()) {
            log.debug("Using default dataset for intent {}: {}", intentType, datasetId);
        }

        return datasetId;
    }

    private String autoMatchKnowledgeBase(IntentType intentType) {
        if (cachedKnowledgeBaseList.isEmpty()) {
            log.debug("No cached knowledge base list for keyword matching");
            return null;
        }

        List<String> keywords = kbProperties.getKeywordsForIntent(intentType.name());
        if (keywords == null || keywords.isEmpty()) {
            log.debug("No keywords defined for intent: {}", intentType);
            return null;
        }

        Map<String, Integer> scoreMap = new HashMap<>();

        for (KnowledgeBaseInfo kb : cachedKnowledgeBaseList) {
            int score = 0;
            String text = (kb.getName() != null ? kb.getName() : "") + " "
                    + (kb.getDescription() != null ? kb.getDescription() : "");

            if (kb.getTags() != null) {
                text += " " + kb.getTags().stream()
                        .map(KnowledgeBaseInfo.Tag::getName)
                        .collect(java.util.stream.Collectors.joining(" "));
            }

            String lowerText = text.toLowerCase();
            for (String keyword : keywords) {
                if (lowerText.contains(keyword.toLowerCase())) {
                    score++;
                }
            }

            if (score > 0) {
                scoreMap.put(kb.getId(), score);
            }
        }

        if (scoreMap.isEmpty()) {
            log.debug("No matching knowledge base found for intent: {}", intentType);
            return null;
        }

        String bestMatchId = Collections.max(scoreMap.entrySet(), Map.Entry.comparingByValue()).getKey();
        KnowledgeBaseInfo bestMatch = cachedKnowledgeBaseList.stream()
                .filter(kb -> kb.getId().equals(bestMatchId))
                .findFirst()
                .orElse(null);

        if (bestMatch != null) {
            log.info("Keyword-matched intent {} to knowledge base '{}' (id: {}, score: {})",
                    intentType, bestMatch.getName(), bestMatchId, scoreMap.get(bestMatchId));
        }

        return bestMatchId;
    }

    @Override
    public List<KnowledgeBaseInfo> getKnowledgeBaseList() {
        return cachedKnowledgeBaseList;
    }

    @Override
    public void refreshKnowledgeBaseList() {
        if (!isEnabled()) {
            log.debug("Dify knowledge base is not enabled, skipping refresh");
            return;
        }

        log.info("Refreshing knowledge base list...");
        KnowledgeBaseListResponse response = difyClient.listKnowledgeBases();
        if (response != null && response.getData() != null) {
            cachedKnowledgeBaseList = response.getData();
            log.info("Knowledge base list refreshed: {} items", cachedKnowledgeBaseList.size());
        } else {
            log.warn("Failed to refresh knowledge base list");
        }
    }

    @Override
    public boolean isEnabled() {
        return difyClient.isEnabled();
    }

    @Scheduled(fixedRateString = "${dify.knowledge-base-refresh-interval-ms:3600000}", initialDelay = 60000)
    public void scheduledRefresh() {
        refreshKnowledgeBaseList();
    }
}