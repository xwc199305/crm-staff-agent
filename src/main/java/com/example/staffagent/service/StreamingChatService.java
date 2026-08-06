package com.example.staffagent.service;

import com.example.staffagent.dify.dto.DifyResponse;
import reactor.core.publisher.Flux;

import java.util.List;

public interface StreamingChatService {

    Flux<String> streamChat(String prompt);

    Flux<String> streamChat(String query, List<DifyResponse.Record> records);
}
