package com.example.staffagent.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactoryBuilder;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.FileSystemSaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.example.staffagent.dto.ChatResponse;
import com.example.staffagent.dto.IntentResult;
import com.example.staffagent.handler.impl.IntentHandlerFactory;
import com.example.staffagent.intent.IntentRecognizer;
import com.example.staffagent.intent.IntentType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Spring AI Alibaba Graph implementation of the intent workflow.
 *
 * <p>State moves through context construction, recognition, conditional tool routing and a
 * fallback node. Spring AI Alibaba's FileSystemSaver persists graph checkpoints by session id
 * so conversations survive a service restart without external memory infrastructure.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IntentWorkflow {

    private static final String DEFAULT_REPLY = "Sorry, we cannot understand your question. Please rephrase.";
    private static final String BUILD_CONTEXT = "build_context";
    private static final String RECOGNIZE_INTENT = "recognize_intent";
    private static final String INVOKE_TOOL = "invoke_tool";
    private static final String FALLBACK = "fallback";
    private static final String NEXT_NODE = "next_node";
    private static final String INTENT_RESULT = "intent_result";
    private static final String REPLY = "reply";
    private static final String CONVERSATION_HISTORY = "conversation_history";
    private static final int MAX_HISTORY_TURNS = 20;

    private final IntentRecognizer intentRecognizer;
    private final IntentHandlerFactory handlerFactory;

    @Value("${conversation.checkpoint-directory:./data/graph-checkpoints}")
    private String checkpointDirectory;

    private CompiledGraph graph;

    @PostConstruct
    void initialize() {
        try {
            StateGraph stateGraph = new StateGraph(new KeyStrategyFactoryBuilder()
                    .addStrategies(Map.of(
                            "user_id", KeyStrategy.REPLACE,
                            "session_id", KeyStrategy.REPLACE,
                            "user_input", KeyStrategy.REPLACE,
                            "conversation_context", KeyStrategy.REPLACE,
                            INTENT_RESULT, KeyStrategy.REPLACE,
                            NEXT_NODE, KeyStrategy.REPLACE,
                            REPLY, KeyStrategy.REPLACE,
                            CONVERSATION_HISTORY, KeyStrategy.REPLACE))
                    .defaultStrategy(KeyStrategy.REPLACE)
                    .build())
                    .addNode(BUILD_CONTEXT, node_async(this::buildContext))
                    .addNode(RECOGNIZE_INTENT, node_async(this::recognizeIntent))
                    .addNode(INVOKE_TOOL, node_async(this::invokeTool))
                    .addNode(FALLBACK, node_async(this::fallback))
                    .addEdge(StateGraph.START, BUILD_CONTEXT)
                    .addEdge(BUILD_CONTEXT, RECOGNIZE_INTENT)
                    .addConditionalEdges(RECOGNIZE_INTENT,
                            edge_async(state -> state.value(NEXT_NODE, String.class).orElse(FALLBACK)),
                            Map.of(INVOKE_TOOL, INVOKE_TOOL, FALLBACK, FALLBACK))
                    .addEdge(INVOKE_TOOL, StateGraph.END)
                    .addEdge(FALLBACK, StateGraph.END);
            String directory = checkpointDirectory == null || checkpointDirectory.isBlank()
                    ? "./data/graph-checkpoints" : checkpointDirectory;
            FileSystemSaver saver = new FileSystemSaver(Path.of(directory), stateGraph.getStateSerializer());
            graph = stateGraph.compile(CompileConfig.builder()
                    .saverConfig(SaverConfig.builder().register(SaverConstant.MEMORY, saver).build())
                    .build());
            log.info("Spring AI Alibaba intent graph initialized, checkpointDirectory={}", directory);
        } catch (GraphStateException e) {
            throw new IllegalStateException("Unable to initialize intent workflow graph", e);
        }
    }

    public ChatResponse execute(String userId, String userInput, String sessionId) {
        if (graph == null) {
            initialize();
        }
        try {
            OverAllState result = graph.invoke(Map.of(
                            "user_id", userId,
                            "session_id", sessionId,
                            "user_input", userInput),
                    RunnableConfig.builder().threadId(sessionThreadId(userId, sessionId)).build())
                    .orElseThrow(() -> new IllegalStateException("Intent graph returned no state"));

            IntentResult intent = result.value(INTENT_RESULT, IntentResult.class)
                    .orElse(IntentResult.unknown(userInput));
            String reply = result.value(REPLY, String.class).orElse(DEFAULT_REPLY);
            return ChatResponse.builder()
                    .reply(reply)
                    .intentType(intent.getIntentType())
                    .intentDescription(intent.getIntentType().getDescription())
                    .intentConfidence(intent.getConfidence())
                    .build();
        } catch (Exception e) {
            log.error("Intent graph execution failed, sessionId={}", sessionId, e);
            throw new IllegalStateException("Intent workflow execution failed", e);
        }
    }

    private Map<String, Object> buildContext(OverAllState state) {
        String userInput = state.value("user_input", String.class).orElse("");
        List<String> history = history(state);
        String context = String.join("\n", history);
        if (!context.isBlank()) {
            context += "\n";
        }
        context += "用户：" + userInput;
        return Map.of(
                "conversation_context", context,
                CONVERSATION_HISTORY, appendHistory(history, "用户：" + userInput));
    }

    private Map<String, Object> recognizeIntent(OverAllState state) {
        String userInput = state.value("user_input", String.class).orElse("");
        IntentResult intent = intentRecognizer.recognize(userInput);
        if (intent == null) {
            intent = IntentResult.unknown(userInput);
        }
        boolean hasHandler = intent.getIntentType() != IntentType.UNKNOWN
                && handlerFactory.hasHandler(intent.getIntentType());
        return Map.of(INTENT_RESULT, intent, NEXT_NODE, hasHandler ? INVOKE_TOOL : FALLBACK);
    }

    private Map<String, Object> invokeTool(OverAllState state) {
        String userInput = state.value("user_input", String.class).orElse("");
        IntentResult intent = state.value(INTENT_RESULT, IntentResult.class)
                .orElse(IntentResult.unknown(userInput));
        String context = state.value("conversation_context", String.class).orElse("");
        String reply = handlerFactory.handleWithToolCall(userInput, intent.getIntentType(), context);
        return responseState(state, reply);
    }

    private Map<String, Object> fallback(OverAllState state) {
        return responseState(state, DEFAULT_REPLY);
    }

    private Map<String, Object> responseState(OverAllState state, String reply) {
        String safeReply = reply == null || reply.isBlank() ? DEFAULT_REPLY : reply;
        return Map.of(REPLY, safeReply,
                CONVERSATION_HISTORY, appendHistory(history(state), "助手：" + safeReply));
    }

    @SuppressWarnings("unchecked")
    private List<String> history(OverAllState state) {
        return state.value(CONVERSATION_HISTORY)
                .filter(List.class::isInstance)
                .map(value -> ((List<?>) value).stream().map(String::valueOf).toList())
                .orElseGet(List::of);
    }

    private List<String> appendHistory(List<String> existing, String entry) {
        List<String> result = new ArrayList<>(existing);
        result.add(entry);
        int start = Math.max(0, result.size() - MAX_HISTORY_TURNS);
        return List.copyOf(result.subList(start, result.size()));
    }

    private String sessionThreadId(String userId, String sessionId) {
        String identity = (userId == null ? "" : userId) + "\u0000" + (sessionId == null ? "" : sessionId);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(identity.getBytes(StandardCharsets.UTF_8));
    }
}
