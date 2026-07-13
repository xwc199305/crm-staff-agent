package com.example.staffagent.service;

import com.example.staffagent.dify.dto.DifyResponse;

import java.util.List;

public interface RagService {
    String generate(String query, List<DifyResponse.Record> records);
    String generateWithQuery(String query);
}