package com.example.staffagent.tool.impl;

import com.example.staffagent.dify.DifyKnowledgeBaseService;
import com.example.staffagent.intent.IntentRecognizer;
import com.example.staffagent.intent.IntentType;
import com.example.staffagent.service.RagService;
import com.example.staffagent.tool.ToolCallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToolCallServiceImpl implements ToolCallService {

    private final DifyKnowledgeBaseService difyKnowledgeBaseService;
    private final RagService ragService;
    private final IntentRecognizer intentRecognizer;

    @Override
    public String callKnowledgeBase(String query, IntentType intentType) {
        log.info("Calling knowledge base for intent {}, query: {}", intentType, query);
        
        try {
            var records = difyKnowledgeBaseService.retrieveRecordsByIntent(query, intentType);
            if (records.isEmpty()) {
                log.debug("No records found in knowledge base for intent {}", intentType);
                return "No relevant information available";
            }

            return ragService.generate(query, records);
        } catch (Exception e) {
            log.warn("Knowledge base call failed: {}", e.getMessage());
            return "No relevant information available";
        }
    }

    @Override
    public String callMcpOrder(String query) {
        log.info("MCP order inquiry not yet implemented, query: {}", query);
        return "Order inquiry feature is under development, please try again later";
    }

    @Override
    public String callMcpLogistics(String query) {
        log.info("MCP logistics inquiry not yet implemented, query: {}", query);
        return "Logistics inquiry feature is under development, please try again later";
    }

    @Override
    public String callKnowledgeBaseForTool(String query) {
        log.info("Calling knowledge base for tool, query: {}", query);
        
        try {
            var intentResult = intentRecognizer.recognize(query);
            IntentType intentType = intentResult.getIntentType();
            
            if (intentType == IntentType.UNKNOWN) {
                intentType = IntentType.PRODUCT_CONSULTATION;
            }
            
            var records = difyKnowledgeBaseService.retrieveRecordsByIntent(query, intentType);
            if (records.isEmpty()) {
                log.debug("No records found in knowledge base for query {}", query);
                return "No relevant information available";
            }

            return ragService.generate(query, records);
        } catch (Exception e) {
            log.warn("Knowledge base call failed: {}", e.getMessage());
            return "No relevant information available";
        }
    }
}