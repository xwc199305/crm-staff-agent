package com.example.staffagent.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DashScopeVectorServiceImpl implements VectorService {

    @Value("${agent.api-key:}")
    private String apiKey;

    @Value("${memory.embedding-model:text-embedding-v1}")
    private String embeddingModel;

    private final Map<String, float[]> vectorCache = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) {
            return new float[1024];
        }

        if (vectorCache.containsKey(text)) {
            return vectorCache.get(text);
        }

        float[] vector;
        if (isApiKeyConfigured()) {
            try {
                vector = callDashScopeEmbeddingApi(text);
                log.debug("DashScope Embedding API call succeeded, text length={}", text.length());
            } catch (Exception e) {
                log.warn("DashScope Embedding API call failed: {}, using mock vector", e.getMessage());
                vector = generateMockVector(text);
            }
        } else {
            log.debug("API Key not configured, using mock vector");
            vector = generateMockVector(text);
        }

        vectorCache.put(text, vector);
        return vector;
    }

    @Override
    public List<float[]> batchEmbed(List<String> texts) {
        List<float[]> vectors = new ArrayList<>();
        for (String text : texts) {
            vectors.add(embed(text));
        }
        return vectors;
    }

    private boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    private float[] callDashScopeEmbeddingApi(String text) throws Exception {
        String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", embeddingModel);
        requestBody.put("input", text);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Embedding API call failed, status code: " + response.statusCode() + ", response: " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode data = root.get("data");
        JsonNode firstItem = data.get(0);
        JsonNode vectorNode = firstItem.get("embedding");

        float[] vector = new float[vectorNode.size()];
        for (int i = 0; i < vectorNode.size(); i++) {
            vector[i] = (float) vectorNode.get(i).asDouble();
        }

        return vector;
    }

    private float[] generateMockVector(String text) {
        float[] vector = new float[1024];
        int hash = text.hashCode();
        for (int i = 0; i < 1024; i++) {
            vector[i] = (float) ((hash * (i + 17)) % 1000) / 1000.0f;
        }
        return vector;
    }
}
