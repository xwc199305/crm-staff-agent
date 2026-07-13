# Conversation Memory Management Implementation Proposal

## Background

The current project's session management only stores the last conversation message, with the following issues:
- Cannot maintain multi-turn conversation context
- No semantic association capability for historical conversations
- Token consumption increases with session growth
- Irrelevant historical information interferes with current conversations
- Memory storage does not support distributed deployment and data persistence

## Goals

Implement conversation memory management mechanism, including:
1. **Qdrant Vector Database Integration**: Introduce lightweight vector database for efficient vector storage and retrieval
2. **Vectorized Storage of Historical Conversations**: Convert user and assistant historical messages to vectors and store in Qdrant
3. **Automatic Summary Generation**: Trigger historical message summarization based on Token threshold to reduce context length
4. **Semantic Association Retrieval**: Automatically select relevant history through Qdrant vector similarity to avoid irrelevant context interference

## Non-Goals

- Not implementing vector database high-availability cluster deployment (single node meets current requirements)
- Not implementing cross-session global semantic retrieval (only session-internal retrieval)
- Not introducing multiple vector database adapters (focus on Qdrant)

## Core Capabilities

### Capability 1: Qdrant Vector Database Integration

- Auto-initialize Qdrant connection on application startup
- Create dedicated Collection for conversation message vectors
- Build HNSW index for efficient similarity search
- Support graceful degradation to memory storage on connection failure

### Capability 2: Vectorized Storage of Historical Conversations

- Convert each historical message (user input, assistant response) to vector representation
- Use DashScope Embedding API for vectorization
- Store vectors along with message metadata (session_id, role, content, timestamp) in Qdrant
- Support vector cache reuse to reduce API calls

### Capability 3: Semantic Retrieval of Historical Conversations

- Receive current user query, vectorize and perform similarity search through Qdrant
- Filter by session_id to ensure only current session messages are retrieved
- Return Top-K most relevant historical messages
- Support configurable similarity threshold and return count

### Capability 4: Token Threshold Auto Summary

- Monitor total Token count of historical conversations
- Automatically call LLM to generate summary when threshold is exceeded
- Summary replaces old historical messages while preserving key information
- Support manual trigger for summary generation

### Capability 5: Context Assembly

- Combine relevant history retrieved from Qdrant with latest messages to assemble context
- Ensure context stays within model Token limits
- Prioritize recent and most relevant messages

## Impact Scope

| Module | Impact |
|--------|--------|
| `vector/QdrantConfig` | New: Qdrant configuration class |
| `vector/VectorStore` | New: Vector storage interface |
| `vector/QdrantVectorStore` | New: Qdrant vector storage implementation |
| `vector/VectorService` | New: Vectorization service interface |
| `vector/DashScopeVectorServiceImpl` | New: DashScope vectorization implementation |
| `context/ChatMessage` | New: Chat message class |
| `context/ConversationContext` | Extended: Support full conversation history |
| `context/ConversationMemoryManager` | New: Memory manager |
| `service/SummaryGenerator` | New: Summary generator |
| `service/ReactAgentServiceImpl` | Modified: Integrate memory management |
| `service/ReactAgentWithToolsServiceImpl` | Modified: Integrate memory management |
| `application.properties` | Modified: Add Memory and Qdrant configuration |
| `docker-compose.yml` | New: Qdrant container deployment configuration (optional) |

## Technology Selection

### Vector Database Selection Analysis

Based on local deployment scenarios, evaluate mainstream vector databases:

| Database | Docker Deployment | Java Client | Resource Usage | Retrieval Performance | Dependencies |
|----------|-------------------|-------------|----------------|----------------------|--------------|
| **Qdrant** | ✅ Official support | ✅ Community SDK | **Lightweight** | High | **None** (single container) |
| Milvus | ✅ Official support | ✅ Official SDK | Medium | High | etcd + MinIO |
| Weaviate | ✅ Official support | ✅ Official SDK | Medium | Medium | Multiple components |
| PostgreSQL+pgvector | ✅ Supported | ✅ JDBC | Medium | Medium | PostgreSQL |

### Selection Decisions

| Component | Selection | Reason |
|-----------|-----------|--------|
| Vector Model | DashScope Embedding | Existing API Key configured, no additional dependencies |
| **Vector Database** | **Qdrant** | Lightweight single-container deployment, no dependencies, simple configuration, complete Java SDK, suitable for local development |
| Index Type | HNSW | Excellent query performance, fast construction, suitable for local small-to-medium data |
| Similarity Metric | Cosine Similarity | Commonly used for text vectors, natively supported by Qdrant |
| Summary Generation | Qwen-Max LLM | Existing model configuration, good results |

### Detailed Selection Reasons

**Why Qdrant over other solutions:**

| Comparison | Qdrant | Milvus | PostgreSQL+pgvector |
|------------|--------|--------|---------------------|
| Deployment Complexity | **Very low** (single container) | Medium (requires etcd+MinIO) | Medium |
| Resource Usage | **Lightweight** (512MB RAM sufficient) | Medium (4GB+ recommended) | Medium |
| Dependencies | **None** | Needs etcd, MinIO | Needs PostgreSQL |
| Java SDK | Community maintained, complete API | Officially maintained, complete API | Via JDBC |
| Query Performance (10k scale) | Millisecond level | Millisecond level | Hundred millisecond level |
| Index Algorithms | HNSW (recommended), IVF_FLAT | Rich | Limited |

**Qdrant Advantages:**
- Single-container deployment, ready to use
- Embedded storage (RocksDB), data persistence
- RESTful API + gRPC dual protocol support
- Built-in visual management interface
- Support for vector filtering (Payload Filtering)
- Support for sharding and replication (production-ready)

## Local Deployment

### Docker Deployment (Recommended)

**Development Environment**: Use Docker to start single-node Qdrant, single container with no dependencies:

```bash
# Start Qdrant (detached mode)
docker run -p 6333:6333 -p 6334:6334 \
  -v $(pwd)/qdrant_storage:/qdrant/storage \
  qdrant/qdrant:latest

# Or use docker-compose
```

### Docker Compose Configuration

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

### Startup Commands

```bash
# Create data directory
mkdir -p qdrant_storage

# Start Qdrant
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker logs qdrant

# Stop
docker-compose down
```

### Local Direct Connection

If Qdrant is running locally, the application connects directly:
- HTTP interface: `http://localhost:6333`
- gRPC interface: `http://localhost:6334`

### Resource Requirements

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| CPU | 1 core | 2 cores |
| Memory | 512MB | 2GB |
| Disk | 10GB | 30GB |

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Qdrant connection failure | Degrade to memory storage, no impact on core business |
| Vector generation time-consuming | Async generation, does not block user requests |
| Index construction time-consuming | Build on application startup, support brute-force search fallback |
| Increased Token consumption | Summary mechanism auto-compresses context |
| API call failure | Fallback to traditional session management |
| Excessive memory usage | Configure max history message limit, periodically clean expired sessions |
| Qdrant service unavailable | Auto-degrade to memory storage mode, record alert logs |
