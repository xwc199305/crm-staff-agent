package com.example.staffagent.dify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DifyRequest {
    private String query;
    private RetrievalModel retrieval_model;
    private ExternalRetrievalModel external_retrieval_model;
    private List<String> attachment_ids;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrievalModel {
        private String search_method;
        private Integer top_k;
        private Double score_threshold;
        private Boolean score_threshold_enabled;
        private Boolean reranking_enable;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalRetrievalModel {
        private Integer top_k;
        private Double score_threshold;
        private Boolean score_threshold_enabled;
    }
}