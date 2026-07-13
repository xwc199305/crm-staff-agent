package com.example.staffagent.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryStats {
    private String sessionId;
    private int messageCount;
    private int tokenCount;
    private boolean hasSummary;
    private int vectorCount;
}
