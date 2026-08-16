package com.example.staffagent.controller;

import com.example.staffagent.intent.IntentType;
import com.example.staffagent.mcp.SalesforceMcpRequestContext;
import com.example.staffagent.service.ReactAgentService;
import com.example.staffagent.service.StreamingChatService;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReactAgentControllerTest {

    @Test
    void chatWithIntentUsesRequestSalesforceHeadersOnlyForThatRequest() {
        ReactAgentService agentService = mock(ReactAgentService.class);
        ReactAgentController controller = new ReactAgentController(
                agentService,
                mock(StreamingChatService.class));
        ReactAgentController.ChatWithIntentRequest request = new ReactAgentController.ChatWithIntentRequest();
        request.setMessage("帮我查看订单号111134135");

        when(agentService.chatWithIntent("default-user", "帮我查看订单号111134135", "default"))
                .thenAnswer(invocation -> {
                    SalesforceMcpRequestContext.Credentials credentials = SalesforceMcpRequestContext.get();
                    assertThat(credentials.orgDomain()).isEqualTo("https://example.my.salesforce.com");
                    assertThat(credentials.accessToken()).isEqualTo("access-token");
                    return com.example.staffagent.dto.ChatResponse.builder()
                            .reply("ok")
                            .intentType(IntentType.ORDER_INQUIRY)
                            .intentDescription("Order Inquiry")
                            .intentConfidence(1.0)
                            .build();
                });

        controller.chatWithIntent(
                "https://example.my.salesforce.com", "Bearer access-token", request);

        assertThat(SalesforceMcpRequestContext.get()).isNull();
    }

    @Test
    void chatWithIntentStreamUsesTheSameGraphBackedToolWorkflow() {
        ReactAgentService agentService = mock(ReactAgentService.class);
        StreamingChatService streamingChatService = mock(StreamingChatService.class);
        ReactAgentController controller = new ReactAgentController(
                agentService, streamingChatService);
        ReactAgentController.ChatWithIntentRequest request = new ReactAgentController.ChatWithIntentRequest();
        request.setUserId("user-1");
        request.setSessionId("session-1");
        request.setMessage("产品如何使用");

        when(agentService.chatWithIntent("user-1", "产品如何使用", "session-1"))
                .thenReturn(com.example.staffagent.dto.ChatResponse.builder()
                        .reply("query tool result")
                        .intentType(IntentType.PRODUCT_CONSULTATION)
                        .intentDescription("Product Consultation")
                        .intentConfidence(0.91)
                        .build());

        List<ServerSentEvent<String>> events = controller.chatWithIntentStream(null, null, request).collectList().block();

        assertThat(events).extracting(ServerSentEvent::event).containsExactly("metadata", "delta", "done");
        assertThat(events.get(1).data()).isEqualTo("query tool result");
        verify(agentService).chatWithIntent("user-1", "产品如何使用", "session-1");
    }
}
