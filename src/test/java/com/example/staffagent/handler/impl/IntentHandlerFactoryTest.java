package com.example.staffagent.handler.impl;

import com.example.staffagent.handler.IntentHandler;
import com.example.staffagent.intent.IntentType;
import com.example.staffagent.tool.ToolCallService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IntentHandlerFactoryTest {

    @Test
    void orderInquiryUsesTheMcpQueryTool() {
        ToolCallService toolCallService = mock(ToolCallService.class);
        IntentHandler orderHandler = new OrderInquiryHandler();
        IntentHandlerFactory factory = new IntentHandlerFactory(List.of(orderHandler), toolCallService);
        factory.init();
        when(toolCallService.callOrder("帮我查看订单号111134135", "")).thenReturn("订单状态：已发货");

        String result = factory.handleWithToolCall("帮我查看订单号111134135", IntentType.ORDER_INQUIRY);

        assertThat(result).isEqualTo("订单状态：已发货");
        verify(toolCallService).callOrder("帮我查看订单号111134135", "");
        verifyNoMoreInteractions(toolCallService);
    }
}
