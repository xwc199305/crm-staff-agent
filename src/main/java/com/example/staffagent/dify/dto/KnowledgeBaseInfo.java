package com.example.staffagent.dify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseInfo {
    private String id;
    private String name;
    private String description;
    private List<Tag> tags;
    private String provider;
    private String permission;
    private Integer document_count;
    private Integer word_count;
    private Boolean enable_api;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tag {
        private String id;
        private String name;
        private String type;
    }

}