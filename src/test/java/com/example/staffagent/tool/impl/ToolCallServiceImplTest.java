package com.example.staffagent.tool.impl;

import com.example.staffagent.dify.DifyKnowledgeBaseService;
import com.example.staffagent.intent.IntentRecognizer;
import com.example.staffagent.mcp.McpClient;
import com.example.staffagent.service.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ToolCallServiceImplTest {

    @Test
    void orderInquiryFormatsSuccessfulMcpDataWithLlm() {
        McpClient mcpClient = mock(McpClient.class);
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        ToolCallServiceImpl service = new ToolCallServiceImpl(
                mock(DifyKnowledgeBaseService.class), mock(RagService.class), mock(IntentRecognizer.class),
                mcpClient, chatClientBuilder);
        String soql = "SELECT Id, OrderNumber, Status, EffectiveDate, TotalAmount, Account.Name "
                + "FROM Order WHERE OrderNumber = '111134135' LIMIT 1";
        String rawResult = "{\"totalSize\":1,\"records\":[{\"OrderNumber\":\"111134135\",\"Status\":\"Activated\"}]}";
        when(mcpClient.callTool("querySoql", Map.of("soql", soql))).thenReturn(rawResult);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("订单 111134135 当前状态为 Activated。");

        String result = service.callOrder("帮我查看这个订单号111134135", "用户此前询问过订单物流");

        assertThat(result).isEqualTo("订单 111134135 当前状态为 Activated。");
        verify(requestSpec).user(contains(rawResult));
    }

    @Test
    void orderInquiryCallsSalesforceQuerySoqlWithAParameterizedOrderNumber() {
        McpClient mcpClient = mock(McpClient.class);
        ToolCallServiceImpl service = new ToolCallServiceImpl(
                mock(DifyKnowledgeBaseService.class),
                mock(RagService.class),
                mock(IntentRecognizer.class),
                mcpClient,
                mock(ChatClient.Builder.class));
        String soql = "SELECT Id, OrderNumber, Status, EffectiveDate, TotalAmount, Account.Name "
                + "FROM Order WHERE OrderNumber = '111134135' LIMIT 1";
        when(mcpClient.callTool("querySoql", Map.of("soql", soql))).thenReturn("{\"totalSize\":1}");

        String result = service.callOrder("帮我查看这个订单号111134135");

        assertThat(result).isEqualTo("{\"totalSize\":1}");
        verify(mcpClient).callTool("querySoql", Map.of("soql", soql));
    }

    @Test
    void orderInquiryDoesNotSendSoqlWhenNoOrderNumberIsPresent() {
        McpClient mcpClient = mock(McpClient.class);
        ToolCallServiceImpl service = new ToolCallServiceImpl(
                mock(DifyKnowledgeBaseService.class),
                mock(RagService.class),
                mock(IntentRecognizer.class),
                mcpClient,
                mock(ChatClient.Builder.class));

        assertThat(service.callOrder("帮我查一下订单状态"))
                .isEqualTo("无法识别订单号，请提供订单号后再查询。");
        verifyNoInteractions(mcpClient);
    }
}
