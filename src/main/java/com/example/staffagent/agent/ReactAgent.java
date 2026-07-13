package com.example.staffagent.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import reactor.core.publisher.Mono;

/**
 * ReAct Agent wrapper class based on AgentScope Java
 * Provides thinking-acting-observing loop reasoning capability
 */
public class ReactAgent {
    private final ReActAgent agent;
    private final String name;

    /**
     * Constructor
     * 
     * @param name Agent name
     * @param apiKey DashScope API Key
     */
    public ReactAgent(String name, String apiKey) {
        this.name = name;
        
        // Create model
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName("qwen-max")
                .build();
        
        // Build ReActAgent (version without tools)
        this.agent = ReActAgent.builder()
                .name(name)
                .sysPrompt("You are a helpful AI assistant.")
                .model(model)
                .build();
    }

    /**
     * Send message to Agent and get response
     * 
     * @param userInput User input
     * @return Agent response
     */
    public String call(String userInput) {
        Msg msg = Msg.builder()
                .textContent(userInput)
                .build();
        
        Mono<Msg> responseMono = agent.call(msg);
        Msg response = responseMono.block();
        
        return response != null ? response.getTextContent() : "Sorry, I cannot generate a response.";
    }

    /**
     * Get Agent name
     * 
     * @return Agent name
     */
    public String getName() {
        return name;
    }
}