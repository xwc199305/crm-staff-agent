package com.example.staffagent.dify;

import com.example.staffagent.dify.dto.KnowledgeBaseInfo;
import com.example.staffagent.intent.IntentType;

import java.util.List;

public interface KnowledgeBaseMatcher {
    String match(IntentType intentType, String query, List<KnowledgeBaseInfo> knowledgeBaseList);
}