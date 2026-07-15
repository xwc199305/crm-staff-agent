package com.example.staffagent.service;

import com.example.staffagent.dto.ChatResponse;
import com.example.staffagent.dto.IntentResult;

public interface ReactAgentService {
    String call(String userInput);
    ChatResponse chatWithIntent(String userId, String userInput, String sessionId);
    IntentResult recognizeIntent(String userInput);
    String getName();
}