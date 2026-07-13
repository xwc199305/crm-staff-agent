package com.example.staffagent.vector;

import com.example.staffagent.context.ChatMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class QdrantVectorStore implements VectorStore {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final Map<String, List<ChatMessage>> memoryCache = new ConcurrentHashMap<>();

    @Value("${qdrant.host:localhost}")
    private String host;

    @Value("${qdrant.port:6333}")
    private int port;

    @Value("${qdrant.collection-name:conversation_messages}")
    private String collectionName;

    @Value("${qdrant.vector-dimensions:1024}")
    private int vectorDimensions;

    @Value("${qdrant.distance:cosine}")
    private String distance;

    @Value("${qdrant.hnsw-m:16}")
    private int hnswM;

    @Value("${qdrant.hnsw-ef-construct:100}")
    private int hnswEfConstruct;

    @Value("${qdrant.auto-create-collection:true}")
    private boolean autoCreateCollection;

    private String baseUrl;

    @PostConstruct
    public void init() {
        baseUrl = String.format("http://%s:%d", host, port);
        try {
            if (autoCreateCollection) {
                try {
                    createCollectionIfNotExists();
                } catch (Exception createEx) {
                    log.warn("Failed to create collection, checking if it exists: {}", createEx.getMessage());
                    String url = baseUrl + "/collections/" + collectionName;
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .GET()
                            .build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        log.info("Collection already exists, skipping creation");
                    } else {
                        throw createEx;
                    }
                }
            }
            connected.set(true);
            log.info("QdrantVectorStore initialized successfully, baseUrl={}, collection={}", baseUrl, collectionName);
        } catch (Exception e) {
            log.error("QdrantVectorStore initialization failed, will use memory mode: {}", e.getMessage());
            connected.set(false);
        }
    }

    private void createCollectionIfNotExists() throws IOException, InterruptedException {
        String url = baseUrl + "/collections/" + collectionName;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            log.info("Creating Qdrant collection: {}", collectionName);

            Map<String, Object> vectorsConfig = new LinkedHashMap<>();
            vectorsConfig.put("size", vectorDimensions);
            vectorsConfig.put("distance", distance.substring(0, 1).toUpperCase() + distance.substring(1).toLowerCase());

            Map<String, Object> hnswConfig = new LinkedHashMap<>();
            hnswConfig.put("m", hnswM);
            hnswConfig.put("ef_construct", hnswEfConstruct);

            Map<String, Object> payloadField = new LinkedHashMap<>();
            payloadField.put("type", "keyword");

            Map<String, Object> payloadIndex = new LinkedHashMap<>();
            payloadIndex.put("session_id", payloadField);

            Map<String, Object> createRequest = new LinkedHashMap<>();
            createRequest.put("vectors", vectorsConfig);
            createRequest.put("hnsw_config", hnswConfig);
            createRequest.put("payload_index", payloadIndex);

            String jsonBody = objectMapper.writeValueAsString(createRequest);
            log.info("Collection creation request JSON: {}", jsonBody);

            HttpRequest createRequestHttp = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(createRequest)))
                    .build();

            HttpResponse<String> createResponse = httpClient.send(createRequestHttp, HttpResponse.BodyHandlers.ofString());

            if (createResponse.statusCode() == 200) {
                log.info("Qdrant collection created successfully: {}", collectionName);
            } else {
                throw new RuntimeException("Collection creation failed, status code: " + createResponse.statusCode() + ", response: " + createResponse.body());
            }
        } else {
            log.info("Qdrant collection already exists: {}", collectionName);
        }
    }

    @Override
    public void insert(List<ChatMessage> messages) {
        if (!connected.get()) {
            log.warn("Qdrant not connected, using memory cache mode");
            messages.forEach(msg -> memoryCache.computeIfAbsent(msg.getSessionId(), k -> new ArrayList<>()).add(msg));
            return;
        }

        try {
            String url = baseUrl + "/collections/" + collectionName + "/points";

            List<Map<String, Object>> points = new ArrayList<>();
            for (ChatMessage message : messages) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("id", message.getId());

                List<Float> vectorList = new ArrayList<>();
                for (float f : message.getVector()) {
                    vectorList.add(f);
                }
                point.put("vector", vectorList);

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("session_id", message.getSessionId());
                payload.put("role", message.getRole());
                payload.put("content", message.getContent());
                payload.put("timestamp", message.getTimestamp() != null ? message.getTimestamp().toString() : LocalDateTime.now().toString());
                payload.put("token_count", message.getTokenCount());
                point.put("payload", payload);

                points.add(point);
            }

            Map<String, Object> upsertRequest = new LinkedHashMap<>();
            upsertRequest.put("points", points);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(upsertRequest)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.debug("Successfully inserted {} vectors to Qdrant", points.size());
            } else {
                log.error("Failed to insert vectors, status code: {}, response: {}", response.statusCode(), response.body());
                messages.forEach(msg -> memoryCache.computeIfAbsent(msg.getSessionId(), k -> new ArrayList<>()).add(msg));
            }
        } catch (Exception e) {
            log.error("Failed to insert vectors to Qdrant: {}", e.getMessage());
            messages.forEach(msg -> memoryCache.computeIfAbsent(msg.getSessionId(), k -> new ArrayList<>()).add(msg));
        }
    }

    @Override
    public List<ChatMessage> search(String sessionId, float[] queryVector, int topK, float threshold) {
        if (!connected.get()) {
            log.warn("Qdrant not connected, retrieving from memory cache");
            return searchFromCache(sessionId, queryVector, topK, threshold);
        }

        try {
            String url = baseUrl + "/collections/" + collectionName + "/points/search";

            List<Float> vectorList = new ArrayList<>();
            for (float f : queryVector) {
                vectorList.add(f);
            }

            Map<String, Object> filterMatch = new LinkedHashMap<>();
            filterMatch.put("key", "session_id");
            filterMatch.put("match", Map.of("value", sessionId));

            Map<String, Object> filter = new LinkedHashMap<>();
            filter.put("must", List.of(filterMatch));

            Map<String, Object> searchRequest = new LinkedHashMap<>();
            searchRequest.put("vector", vectorList);
            searchRequest.put("limit", topK);
            searchRequest.put("filter", filter);
            searchRequest.put("score_threshold", threshold);
            searchRequest.put("with_payload", true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(searchRequest)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode result = root.get("result");

                List<ChatMessage> messages = new ArrayList<>();
                if (result.isArray()) {
                    for (JsonNode point : result) {
                        String id = point.has("id") ? point.get("id").asText() : UUID.randomUUID().toString();
                        JsonNode payload = point.get("payload");

                        ChatMessage message = ChatMessage.builder()
                                .id(id)
                                .sessionId(payload.has("session_id") ? payload.get("session_id").asText() : sessionId)
                                .role(payload.has("role") ? payload.get("role").asText() : "unknown")
                                .content(payload.has("content") ? payload.get("content").asText() : "")
                                .timestamp(payload.has("timestamp") ? LocalDateTime.parse(payload.get("timestamp").asText()) : LocalDateTime.now())
                                .tokenCount(payload.has("token_count") ? payload.get("token_count").asInt() : 0)
                                .build();
                        messages.add(message);
                    }
                }
                log.debug("Retrieved {} relevant messages from Qdrant", messages.size());
                return messages;
            } else {
                log.error("Vector search failed, status code: {}, response: {}", response.statusCode(), response.body());
                return searchFromCache(sessionId, queryVector, topK, threshold);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve from Qdrant: {}", e.getMessage());
            return searchFromCache(sessionId, queryVector, topK, threshold);
        }
    }

    private List<ChatMessage> searchFromCache(String sessionId, float[] queryVector, int topK, float threshold) {
        List<ChatMessage> sessionMessages = memoryCache.getOrDefault(sessionId, new ArrayList<>());
        return sessionMessages.stream().limit(topK).toList();
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        if (!connected.get()) {
            log.warn("Qdrant not connected, cleaning memory cache");
            memoryCache.remove(sessionId);
            return;
        }

        try {
            String url = baseUrl + "/collections/" + collectionName + "/points/delete";

            Map<String, Object> filterMatch = new LinkedHashMap<>();
            filterMatch.put("key", "session_id");
            filterMatch.put("match", Map.of("value", sessionId));

            Map<String, Object> filter = new LinkedHashMap<>();
            filter.put("must", List.of(filterMatch));

            Map<String, Object> deleteRequest = new LinkedHashMap<>();
            deleteRequest.put("filter", filter);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(deleteRequest)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.debug("Successfully deleted all vectors for session {}", sessionId);
            } else {
                log.error("Failed to delete vectors, status code: {}, response: {}", response.statusCode(), response.body());
                memoryCache.remove(sessionId);
            }
        } catch (Exception e) {
            log.error("Failed to delete session vectors: {}", e.getMessage());
            memoryCache.remove(sessionId);
        }
    }

    @Override
    public int countBySessionId(String sessionId) {
        if (!connected.get()) {
            return memoryCache.getOrDefault(sessionId, new ArrayList<>()).size();
        }

        try {
            String url = baseUrl + "/collections/" + collectionName + "/points/count";

            Map<String, Object> filterMatch = new LinkedHashMap<>();
            filterMatch.put("key", "session_id");
            filterMatch.put("match", Map.of("value", sessionId));

            Map<String, Object> filter = new LinkedHashMap<>();
            filter.put("must", List.of(filterMatch));

            Map<String, Object> countRequest = new LinkedHashMap<>();
            countRequest.put("filter", filter);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(countRequest)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.has("result") && root.get("result").has("count") ? root.get("result").get("count").asInt() : 0;
            } else {
                log.error("Statistics failed, status code: {}, response: {}", response.statusCode(), response.body());
                return 0;
            }
        } catch (Exception e) {
            log.error("Failed to count session vectors: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }
}
