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
public class DifyResponse {
    private Query query;
    private List<Record> records;
    private boolean success;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Query {
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Record {
        private Segment segment;
        private List<Object> child_chunks;
        private Double score;
        private Object tsne_position;
        private List<Object> files;
        private Object summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Segment {
        private String id;
        private Integer position;
        private String document_id;
        private String content;
        private String sign_content;
        private String answer;
        private Integer word_count;
        private Integer tokens;
        private List<String> keywords;
        private String index_node_id;
        private String index_node_hash;
        private Integer hit_count;
        private Boolean enabled;
        private Object disabled_at;
        private Object disabled_by;
        private String status;
        private String created_by;
        private Long created_at;
        private Long indexing_at;
        private Long completed_at;
        private Object error;
        private Object stopped_at;
        private Document document;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Document {
        private String id;
        private String data_source_type;
        private String name;
        private Object doc_type;
        private Object doc_metadata;
    }
}