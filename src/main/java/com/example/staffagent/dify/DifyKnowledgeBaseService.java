package com.example.staffagent.dify;

import com.example.staffagent.dify.dto.DifyResponse;
import com.example.staffagent.dify.dto.KnowledgeBaseInfo;
import com.example.staffagent.intent.IntentType;

import java.util.List;
import java.util.Optional;

public interface DifyKnowledgeBaseService {
    Optional<String> query(String query);
    List<DifyResponse.Record> retrieveRecords(String query);
    List<DifyResponse.Record> retrieveRecordsByIntent(String query, IntentType intentType);
    String findDatasetId(String query, IntentType intentType);
    List<KnowledgeBaseInfo> getKnowledgeBaseList();
    void refreshKnowledgeBaseList();
    boolean isEnabled();
}