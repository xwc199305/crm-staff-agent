# Conversation Memory Management Technical Design

## 1. Architecture Design

### 1.1 Overall Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        ConversationMemoryManager                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌───────────────────────┐   │
│  │  VectorStore    │  │  VectorService  │  │  SummaryGenerator     │   │
│  │  (Qdrant Store) │  │  (Vectorization)│  │  (Summary Generator)  │   │
│  └────────┬────────┘  └────────┬────────┘  └────────────┬──────────┘   │
│           │                    │                         │              │
│           ▼                    ▼                         ▼              │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │                    ConversationContext                            │ │
│  │  - messages: List<ChatMessage> (Full conversation history)       │ │
│  │  - summary: String (History summary)                            │ │
│  │  - tokenCount: int (Current token count)                        │ │
│  └───────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          Qdrant Vector Database                        │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  Collection: conversation_messages                              │   │
│  │  - id: String (Message ID)                                      │   │
│  │  - payload: {session_id, role, content, timestamp, token_count} │   │
│  │  - vector: FloatVector (1024 dimensions)                        │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        Service Layer Integration                       │
│  ReactAgentServiceImpl / ReactAgentWithToolsServiceImpl                │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  buildContext(query, sessionId)                                  │   │
│  │    1. Retrieve relevant historical messages from vector DB      │   │
│  │    2. Assemble context (history + summary + current query)      │   │
│  │    3. Check token threshold, generate summary if needed         │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Core Component Responsibilities

| Component | Responsibility |
|-----------|----------------|
| `ConversationMemoryManager` | Unified conversation memory management, coordinate vector storage, retrieval, summary |
| `VectorService` | Call DashScope Embedding API for text vectorization |
| `VectorStore` | Qdrant vector database operations, support insert, query, delete |
| `SummaryGenerator` | Call LLM to generate historical message summaries |
| `ConversationContext` | Extended support for full conversation history |

### 1.3 Qdrant Vector Database Design

**Collection Name**: `conversation_messages`

**Vector Configuration**:

| Property | Value | Description |
|----------|-------|-------------|
| size | 1024 | Vector dimensions |
| distance | Cosine | Cosine similarity metric |

**Payload Schema**:

| Field Name | Type | Description |
|------------|------|-------------|
| session_id | String | Session ID, used for filtering queries |
| role | String | Role: user/assistant/system |
| content | String | Message content |
| timestamp | Long | Creation timestamp |
| token_count | Integer | Token count |

**Index Configuration**:

| Index Type | Parameters | Description |
|------------|------------|-------------|
| HNSW | m=16, ef_construct=100 | Excellent query performance, suitable for small-to-medium data |

**Payload Index**:

| Field | Index Type | Description |
|-------|------------|-------------|
| session_id | keyword | Used for exact match filtering |
| timestamp | numeric | Used for range queries and sorting |

### 1.4 Local Deployment Architecture

```
┌───────────────────────────────────────────────────────────────────────┐
│                         Local Development Environment                │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌─────────────────┐                                                  │
│  │   Qdrant        │                                                  │
│  │  (Vector Store) │                                                  │
│  │  6333/tcp (HTTP)│                                                  │
│  │  6334/tcp (gRPC)│                                                  │
│  └────────┬────────┘                                                  │
│           │                                                           │
│           ▼                                                           │
│  ┌─────────────────┐                                                  │
│  │  Spring Boot    │                                                  │
│  │  Application    │                                                  │
│  │  8080/tcp       │                                                  │
│  └─────────────────┘                                                  │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

**Local Deployment Component Description**:

| Component | Role | Port | Storage |
|-----------|------|------|---------|
| Qdrant | Vector storage and retrieval | 6333 (HTTP), 6334 (gRPC) | Local directory mount |
| Spring Boot | Business application | 8080 | - |

**Qdrant Visual Management Interface**:
- Address: `http://localhost:6333/dashboard`
- Features: View Collections, query vectors, manage indexes

### 1.5 Resource Requirements

| Resource | Minimum | Recommended | Description |
|----------|---------|-------------|-------------|
| CPU | 1 core | 2 cores | Qdrant vector retrieval consumes CPU |
| Memory | 512MB | 2GB | HNSW index needs memory |
| Disk | 10GB | 30GB | Store vectors and metadata |

## 2. Data Structure Design

### 2.1 ChatMessage

```java
public class ChatMessage {
    private String id;              // Message unique ID
    private String role;            // Role: user/assistant/system
    private String content;         // Message content
    private LocalDateTime timestamp; // Timestamp
    private float[] vector;         // Vector representation
    private int tokenCount;         // Token count
}
```

### 2.2 ConversationContext (Extended)

```java
public class ConversationContext {
    private String sessionId;
    private List<ChatMessage> messages;         // Full conversation history
    private String summary;                     // History summary
    private int tokenCount;                     // Current total token count
    private LocalDateTime lastActiveTime;
    private int turnCount;
}
```

### 2.3 MemoryStats

```java
public class MemoryStats {
    private String sessionId;
    private int messageCount;
    private int tokenCount;
    private boolean hasSummary;
    private int vectorCount;
}
```

## 3. Core Algorithm Design

### 3.1 Vectorization Flow

```
User Message → VectorService.embed(text) → DashScope Embedding API → float[] vector → Qdrant insert
```

### 3.2 Semantic Retrieval Flow

```
Current Query → VectorService.embed(query) → Qdrant.search() → Return Top-K Similar Messages
```

**Qdrant Query Parameters**:

| Parameter | Value | Description |
|-----------|-------|-------------|
| collectionName | conversation_messages | Collection name |
| vector | queryVector | Query vector |
| limit | topK | Return count |
| filter | {session_id: {eq: sessionId}} | Session filter condition |
| scoreThreshold | threshold | Similarity threshold |
| withPayload | true | Return payload data |

### 3.3 Vector Storage Operation Flow

**Insert Flow**:
```
Message Created → Vectorization → Qdrant.upsert() → Update ConversationContext
```

**Query Flow**:
```
Query Vectorization → Qdrant.search() → Parse Results → Return ChatMessage List
```

**Delete Flow**:
```
Session Ended → Qdrant.delete(where: {session_id: {eq: sessionId}}) → Clean Context
```

### 3.4 Summary Generation Trigger Mechanism

```
Add New Message → Calculate Total Token Count → Check if Threshold Exceeded → 
    Yes → Call SummaryGenerator to Generate Summary → Replace Old Messages → Update Context
    No → Save Message Directly
```

**Summary Prompt**:

```
You are a conversation summary assistant. Please summarize the following conversation history into a concise summary, preserving key information:

{Conversation History}

Please generate the summary in English:
```

## 4. API Design

### 4.1 ConversationMemoryManager Interface

| Method | Description | Parameters | Return Value |
|--------|-------------|------------|--------------|
| `addMessage(sessionId, role, content)` | Add new message | sessionId, role, content | void |
| `retrieveRelevantMessages(sessionId, query, topK, threshold)` | Semantic retrieval of relevant messages | sessionId, query, topK, threshold | List\<ChatMessage\> |
| `buildContext(sessionId, query)` | Assemble conversation context | sessionId, query | String |
| `generateSummary(sessionId)` | Manually trigger summary generation | sessionId | String |
| `clearMemory(sessionId)` | Clear session memory | sessionId | void |
| `getMemoryStats(sessionId)` | Get memory statistics | sessionId | MemoryStats |

### 4.2 VectorStore Interface

| Method | Description | Parameters | Return Value |
|--------|-------------|------------|--------------|
| `init()` | Initialize connection, create Collection and index | None | void |
| `insert(messages)` | Insert message vectors | messages: List\<ChatMessage\> | void |
| `search(sessionId, queryVector, topK, threshold)` | Semantic retrieval | sessionId, queryVector, topK, threshold | List\<ChatMessage\> |
| `deleteBySessionId(sessionId)` | Delete all messages for a session | sessionId: String | void |
| `countBySessionId(sessionId)` | Count messages for a session | sessionId: String | int |
| `isConnected()` | Check connection status | None | boolean |

### 4.3 VectorService Interface

| Method | Description | Parameters | Return Value |
|--------|-------------|------------|--------------|
| `embed(text)` | Convert text to vector | text: String | float[] |
| `batchEmbed(texts)` | Batch vectorization | texts: List\<String\> | List\<float[]\> |

## 5. Configuration Design

### 5.1 Memory Configuration

| Configuration | Default | Description |
|---------------|---------|-------------|
| `memory.vector-dimensions` | 1024 | Vector dimensions |
| `memory.max-history-count` | 50 | Max history message count |
| `memory.max-tokens` | 8000 | Max token threshold |
| `memory.top-k` | 5 | Semantic retrieval return count |
| `memory.similarity-threshold` | 0.5 | Similarity threshold |
| `memory.embedding-model` | text-embedding-v1 | Embedding model name |
| `memory.summary-enabled` | true | Enable auto summary |
| `memory.async-embedding` | true | Enable async vectorization |

### 5.2 Qdrant Vector Database Configuration

| Configuration | Default | Description |
|---------------|---------|-------------|
| `qdrant.host` | localhost | Qdrant service address (local deployment) |
| `qdrant.port` | 6333 | Qdrant HTTP port |
| `qdrant.grpc-port` | 6334 | Qdrant gRPC port |
| `qdrant.collection-name` | conversation_messages | Collection name |
| `qdrant.vector-dimensions` | 1024 | Vector dimensions |
| `qdrant.distance` | cosine | Similarity metric type |
| `qdrant.index-type` | hnsw | Index type |
| `qdrant.hnsw-m` | 16 | HNSW index parameter |
| `qdrant.hnsw-ef-construct` | 100 | HNSW construction parameter |
| `qdrant.timeout-seconds` | 30 | Connection timeout |
| `qdrant.auto-create-collection` | true | Auto create collection |

### 5.3 Configuration Example (application.properties)

```properties
# Memory Configuration
memory.vector-dimensions=1024
memory.max-history-count=50
memory.max-tokens=8000
memory.top-k=5
memory.similarity-threshold=0.5
memory.embedding-model=text-embedding-v1
memory.summary-enabled=true
memory.async-embedding=true

# Qdrant Configuration (Local Deployment)
qdrant.host=localhost
qdrant.port=6333
qdrant.grpc-port=6334
qdrant.collection-name=conversation_messages
qdrant.distance=cosine
qdrant.index-type=hnsw
qdrant.hnsw-m=16
qdrant.hnsw-ef-construct=100
qdrant.timeout-seconds=30
qdrant.auto-create-collection=true
```

### 5.4 Docker Compose Configuration (docker-compose.yml)

```yaml
version: '3.5'

services:
  qdrant:
    container_name: qdrant
    image: qdrant/qdrant:latest
    ports:
      - "6333:6333"
      - "6334:6334"
    volumes:
      - ./qdrant_storage:/qdrant/storage
    environment:
      QDRANT__SERVICE__GRPC_PORT: 6334
      QDRANT__SERVICE__HTTP_PORT: 6333
```

### 5.5 Local Startup Commands

```bash
# Create data directory
mkdir -p qdrant_storage

# Start Qdrant (detached mode)
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker logs qdrant

# Stop
docker-compose down

# Stop and clean data
docker-compose down -v
```

## 6. Implementation Steps

### Phase 1: Qdrant Dependencies and Configuration

1. Add Qdrant Java SDK dependency to pom.xml
2. Create Qdrant configuration class `QdrantConfig`
3. Implement Qdrant client initialization and connection management

### Phase 2: Vector Database Operation Layer

1. Create `VectorStore` interface
2. Implement `QdrantVectorStore` class
3. Implement Collection creation, index building, insert, query, delete operations

### Phase 3: Data Structure Extension

1. Create `ChatMessage` class
2. Extend `ConversationContext` class (remove messageVectors, depend on Qdrant instead)

### Phase 4: Vector Service Implementation

1. Create `VectorService` interface
2. Implement `DashScopeVectorServiceImpl`

### Phase 5: Memory Manager Implementation

1. Create `ConversationMemoryManager` class
2. Implement `addMessage` (vectorization + Qdrant insert)
3. Implement `retrieveRelevantMessages` (Qdrant semantic retrieval)
4. Implement `buildContext` (context assembly)
5. Implement `generateSummary` (summary generation)
6. Implement auto summary trigger mechanism

### Phase 6: Service Layer Integration

1. Modify `ReactAgentServiceImpl` to integrate memory management
2. Modify `ReactAgentWithToolsServiceImpl` to integrate memory management

### Phase 7: Configuration and Testing

1. Add Memory and Qdrant configuration to `application.properties`
2. Add Qdrant container startup script (docker-compose)
3. Compilation verification
4. Startup verification
5. API interface testing

## 7. Error Handling

| Scenario | Handling |
|----------|----------|
| Embedding API call failure | Skip vectorization, save message normally, fall back to full-text match during retrieval |
| Inconsistent vector dimensions | Use default vector, record warning log |
| Summary generation failure | Keep original messages, record warning log |
| Qdrant connection failure | Degrade to memory storage, record error log |
| Qdrant insert failure | Save message to memory buffer, async retry |
| Qdrant query failure | Return empty list, record warning log |
| Qdrant index build failure | Use brute-force search, record warning log |

## 8. Performance Optimization

1. **Async Vectorization**: Generate vectors asynchronously after message saving, do not block user requests
2. **Vector Cache**: Reuse vectors for identical text, reduce API calls
3. **Incremental Update**: Vectorize only new messages
4. **Summary Compression**: Periodically compress historical messages, reduce token consumption
5. **Qdrant Batch Insert**: Batch insert after accumulating multiple messages, reduce network round-trips
6. **Session-level Filtering**: Filter by session_id during queries, avoid cross-session retrieval
7. **Index Optimization**: Adjust HNSW parameters based on data volume, balance index build and query performance
8. **Connection Pool**: Reuse Qdrant client connections, reduce connection overhead
