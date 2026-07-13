package com.example.staffagent.dify;

import com.example.staffagent.dify.dto.DifyRequest;
import com.example.staffagent.dify.dto.DifyResponse;
import com.example.staffagent.dify.dto.KnowledgeBaseListResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DifyClient {

    private final DifyFeignClient difyFeignClient;

    @Value("${dify.api-key:}")
    private String apiKey;

    @Value("${dify.dataset-id:}")
    private String datasetId;

    @Value("${dify.enabled:true}")
    private boolean enabled;

    @Value("${dify.top-k:3}")
    private Integer topK;

    @Value("${dify.score-threshold:0.5}")
    private Double scoreThreshold;

    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isEmpty();
    }

    public DifyResponse retrieve(String query) {
        return retrieve(query, datasetId);
    }

    public DifyResponse retrieve(String query, String targetDatasetId) {
        if (!isEnabled()) {
            log.debug("Dify is disabled or not configured");
            return DifyResponse.builder()
                    .success(false)
                    .error("Dify not configured or disabled")
                    .build();
        }

        if (targetDatasetId == null || targetDatasetId.isEmpty()) {
            log.warn("No dataset ID provided for retrieval");
            return DifyResponse.builder()
                    .success(false)
                    .error("No dataset ID provided")
                    .build();
        }

        DifyRequest request = DifyRequest.builder()
                .query(query)
                .retrieval_model(DifyRequest.RetrievalModel.builder()
                        .search_method("hybrid_search")
                        .top_k(topK)
                        .score_threshold(scoreThreshold)
                        .score_threshold_enabled(true)
                        .reranking_enable(true)
                        .build())
                .build();

        String authorization = "Bearer " + apiKey;

        try {
            log.debug("Calling Dify retrieval API with datasetId: {}", targetDatasetId);
            DifyResponse response = difyFeignClient.retrieve(targetDatasetId, authorization, request);
            response.setSuccess(true);
            log.debug("Dify API response: records={}", response.getRecords() != null ? response.getRecords().size() : 0);
            return response;
        } catch (FeignException e) {
            log.error("Dify API call failed", e);
            return DifyResponse.builder()
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    public KnowledgeBaseListResponse listKnowledgeBases() {
        if (!isEnabled()) {
            log.debug("Dify is disabled or not configured");
            return null;
        }

        String authorization = "Bearer " + apiKey;

        try {
            log.debug("Calling Dify list datasets API");
            KnowledgeBaseListResponse response = difyFeignClient.listDatasets(authorization, 1, 100);
            log.info("Dify knowledge base list: {} items", response != null && response.getData() != null ? response.getData().size() : 0);
            return response;
        } catch (FeignException e) {
            log.error("Dify list datasets API call failed", e);
            return null;
        }
    }

    public String getDefaultDatasetId() {
        return datasetId;
    }
}