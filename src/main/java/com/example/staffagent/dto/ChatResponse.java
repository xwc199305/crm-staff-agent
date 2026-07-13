package com.example.staffagent.dto;

import com.example.staffagent.intent.IntentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String reply;
    private IntentType intentType;
    private String intentDescription;
    private Double intentConfidence;
}