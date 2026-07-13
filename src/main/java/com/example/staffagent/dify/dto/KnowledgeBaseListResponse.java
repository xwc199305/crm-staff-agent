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
public class KnowledgeBaseListResponse {
    private List<KnowledgeBaseInfo> data;
    private boolean has_more;
    private int limit;
    private int total;
    private int page;
}