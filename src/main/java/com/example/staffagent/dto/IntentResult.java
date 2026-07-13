package com.example.staffagent.dto;

import com.example.staffagent.intent.IntentType;
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
public class IntentResult {
    private IntentType intentType;
    private Double confidence;
    private List<String> keywords;
    private Map<String, String> params;
    private String originalQuery;

    public static IntentResult unknown(String query) {
        return IntentResult.builder()
                .intentType(IntentType.UNKNOWN)
                .confidence(0.0)
                .originalQuery(query)
                .build();
    }
}