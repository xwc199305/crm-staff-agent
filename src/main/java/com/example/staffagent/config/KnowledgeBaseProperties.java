package com.example.staffagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "kb")
public class KnowledgeBaseProperties {

    private Matcher matcher = new Matcher();

    @Data
    public static class Matcher {
        private Llm llm = new Llm();
    }

    @Data
    public static class Llm {
        private boolean enabled = true;
        private int timeoutSeconds = 15;
        private String modelName = "qwen-max";
        private String promptTemplate = "";
        private Map<String, List<String>> keywords = new HashMap<>();
        private List<String> responsePrefixes = new ArrayList<>(List.of("知识库ID:", "Knowledge Base ID:", "Dataset ID:"));
    }

    public List<String> getKeywordsForIntent(String intentType) {
        return matcher.llm.keywords.getOrDefault(intentType, List.of());
    }

    public List<String> getResponsePrefixes() {
        return matcher.llm.responsePrefixes;
    }
}
