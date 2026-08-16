package com.example.staffagent.graph;

import com.example.staffagent.dto.ChatResponse;
import com.example.staffagent.dto.IntentResult;
import com.example.staffagent.handler.impl.IntentHandlerFactory;
import com.example.staffagent.intent.IntentRecognizer;
import com.example.staffagent.intent.IntentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IntentWorkflowTest {

    @Test
    void graphRoutesRecognizedIntentToRegisteredToolHandler(@TempDir Path checkpointDirectory) {
        IntentRecognizer recognizer = mock(IntentRecognizer.class);
        IntentHandlerFactory handlerFactory = mock(IntentHandlerFactory.class);
        IntentWorkflow workflow = new IntentWorkflow(recognizer, handlerFactory);
        ReflectionTestUtils.setField(workflow, "checkpointDirectory", checkpointDirectory.toString());
        workflow.initialize();

        IntentResult intent = IntentResult.builder()
                .intentType(IntentType.ORDER_INQUIRY)
                .confidence(1.0)
                .originalQuery("帮我查看订单号111134135")
                .build();
        when(recognizer.recognize("帮我查看订单号111134135")).thenReturn(intent);
        when(handlerFactory.hasHandler(IntentType.ORDER_INQUIRY)).thenReturn(true);
        when(handlerFactory.handleWithToolCall(eq("帮我查看订单号111134135"), eq(IntentType.ORDER_INQUIRY), anyString()))
                .thenReturn("order result");

        ChatResponse response = workflow.execute("user-1", "帮我查看订单号111134135", "session-1");

        assertThat(response.getReply()).isEqualTo("order result");
        assertThat(response.getIntentType()).isEqualTo(IntentType.ORDER_INQUIRY);
        verify(handlerFactory).handleWithToolCall(
                eq("帮我查看订单号111134135"), eq(IntentType.ORDER_INQUIRY), contains("用户：帮我查看订单号111134135"));
    }

    @Test
    void graphRestoresSessionHistoryFromItsSpringAiAlibabaCheckpoint(@TempDir Path checkpointDirectory) {
        IntentRecognizer recognizer = mock(IntentRecognizer.class);
        IntentHandlerFactory handlerFactory = mock(IntentHandlerFactory.class);
        IntentResult intent = IntentResult.builder().intentType(IntentType.ORDER_INQUIRY).confidence(1.0).build();
        when(recognizer.recognize(anyString())).thenReturn(intent);
        when(handlerFactory.hasHandler(IntentType.ORDER_INQUIRY)).thenReturn(true);
        when(handlerFactory.handleWithToolCall(anyString(), eq(IntentType.ORDER_INQUIRY), anyString()))
                .thenReturn("订单已发货");

        IntentWorkflow firstInstance = new IntentWorkflow(recognizer, handlerFactory);
        ReflectionTestUtils.setField(firstInstance, "checkpointDirectory", checkpointDirectory.toString());
        firstInstance.initialize();
        firstInstance.execute("user-1", "查询订单号00000100", "session-1");

        IntentWorkflow restartedInstance = new IntentWorkflow(recognizer, handlerFactory);
        ReflectionTestUtils.setField(restartedInstance, "checkpointDirectory", checkpointDirectory.toString());
        restartedInstance.initialize();
        restartedInstance.execute("user-1", "这个订单的状态呢？", "session-1");

        verify(handlerFactory).handleWithToolCall(
                eq("这个订单的状态呢？"), eq(IntentType.ORDER_INQUIRY), contains("订单已发货"));
    }
}
