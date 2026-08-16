package com.example.staffagent.service.impl;

import com.example.staffagent.agent.StaffAgentTools;
import com.example.staffagent.dto.ChatResponse;
import com.example.staffagent.dto.IntentResult;
import com.example.staffagent.graph.IntentWorkflow;
import com.example.staffagent.intent.IntentRecognizer;
import com.example.staffagent.intent.IntentType;
import org.springframework.ai.chat.client.ChatClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReactAgentServiceImplTest {

    @Test
    void chatWithIntentKeepsUsingTheRegisteredQueryToolPath() {
        IntentRecognizer recognizer = mock(IntentRecognizer.class);
        IntentWorkflow intentWorkflow = mock(IntentWorkflow.class);
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        StaffAgentTools staffAgentTools = mock(StaffAgentTools.class);
        ReactAgentServiceImpl service = new ReactAgentServiceImpl(
                recognizer, intentWorkflow, chatClientBuilder, staffAgentTools);

        IntentResult intent = IntentResult.builder()
                .intentType(IntentType.PRODUCT_CONSULTATION)
                .confidence(0.96)
                .build();
        ChatResponse expected = ChatResponse.builder()
                .reply("query tool result")
                .intentType(IntentType.PRODUCT_CONSULTATION)
                .intentDescription(IntentType.PRODUCT_CONSULTATION.getDescription())
                .intentConfidence(intent.getConfidence())
                .build();
        when(intentWorkflow.execute("user-1", "如何使用产品", "session-1")).thenReturn(expected);

        ChatResponse response = service.chatWithIntent("user-1", "如何使用产品", "session-1");

        assertThat(response.getReply()).isEqualTo("query tool result");
        assertThat(response.getIntentType()).isEqualTo(IntentType.PRODUCT_CONSULTATION);
        verify(intentWorkflow).execute("user-1", "如何使用产品", "session-1");
    }
}
