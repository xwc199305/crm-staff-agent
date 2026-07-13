# Conversation Memory Management Specification

## ADDED Requirements

### Requirement: Qdrant Vector Database Integration

The system SHALL integrate Qdrant vector database for storing and retrieving conversation message vectors.

#### Scenario: Qdrant Connection Initialization
- **WHEN** application starts
- **THEN** automatically establish Qdrant connection
- **AND** check if Collection exists
- **AND** create Collection with HNSW index if not exists

#### Scenario: Qdrant Connection Failure Degradation
- **WHEN** Qdrant connection fails
- **THEN** system degrades to in-memory storage mode
- **AND** log error message
- **AND** core business functions are not affected

#### Scenario: Vector Insertion into Qdrant
- **WHEN** message vectorization completes
- **THEN** vector is inserted into Qdrant database
- **AND** message metadata (session_id, role, content, timestamp) is stored together
- **AND** if insertion fails, save to memory buffer and retry asynchronously

### Requirement: Historical Conversation Vectorization Storage

The system SHALL convert user historical conversation messages into vector representations and store them in Qdrant vector database to support subsequent semantic retrieval.

#### Scenario: Add Message and Vectorize
- **WHEN** user sends new message
- **THEN** message is saved to conversation history
- **AND** message content is vectorized asynchronously
- **AND** vector and message metadata are inserted into Qdrant together

#### Scenario: Vector Cache Reuse
- **WHEN** message with identical text content is sent again
- **THEN** reuse existing vector, do not call Embedding API again

### Requirement: Semantic Retrieval of Historical Conversations

The system SHALL perform similarity retrieval through Qdrant vector database and return historical messages relevant to the current query.

#### Scenario: Semantic Retrieval
- **WHEN** user sends query message
- **THEN** query is vectorized
- **AND** similarity search is performed via Qdrant.search()
- **AND** messages are filtered by session_id for current session
- **AND** return Top-K most relevant historical messages

#### Scenario: Similarity Threshold Filtering
- **WHEN** retrieved message similarity is below threshold
- **THEN** message is filtered out, not added to context

### Requirement: Auto Summary Generation Based on Token Threshold

The system SHALL automatically generate summary to replace part of historical messages when total Token count of historical conversations exceeds threshold.

#### Scenario: Auto Trigger Summary
- **WHEN** total Token count exceeds threshold after adding new message
- **THEN** automatically call LLM to generate historical summary
- **AND** old messages are replaced by summary
- **AND** total Token count decreases

#### Scenario: Manual Trigger Summary
- **WHEN** manual summary API is called
- **THEN** immediately generate historical summary

### Requirement: Context Assembly

The system SHALL assemble appropriate conversation context based on current query and relevant history retrieved from Qdrant.

#### Scenario: Assemble Context
- **WHEN** LLM input context needs to be constructed
- **THEN** retrieve relevant historical messages from Qdrant
- **AND** combine with summary and latest messages
- **AND** ensure context is within Token limit

## Data Structures

### ChatMessage

| Field | Type | Description | Constraint |
|--------|------|------|------|
| id | String | Unique message ID | Not null, UUID |
| role | String | Role | Not null, values: user/assistant/system |
| content | String | Message content | Not null |
| timestamp | LocalDateTime | Timestamp | Not null |
| vector | float[] | Vector representation | Nullable |
| tokenCount | int | Token count | Not null, >=0 |

### ConversationContext (Extended)

| Field | Type | Description | Constraint |
|--------|------|------|------|
| sessionId | String | Session ID | Not null |
| messages | List\<ChatMessage\> | Complete conversation history | Nullable |
| summary | String | Historical summary | Nullable |
| tokenCount | int | Current total Token count | Not null, >=0 |
| lastActiveTime | LocalDateTime | Last active time | Not null |
| turnCount | int | Conversation turns | Not null, >=0 |

### MemoryStats

| Field | Type | Description | Constraint |
|--------|------|------|------|
| sessionId | String | Session ID | Not null |
| messageCount | int | Message count | >=0 |
| tokenCount | int | Total Token count | >=0 |
| hasSummary | boolean | Has summary | Not null |
| vectorCount | int | Vectorized message count | >=0 |

## Configuration

### Memory Configuration

| Item | Default | Description |
|--------|--------|------|
| memory.vector-dimensions | 1024 | Vector dimensions |
| memory.max-history-count | 50 | Maximum historical message count |
| memory.max-tokens | 8000 | Maximum Token threshold |
| memory.top-k | 5 | Semantic retrieval return count |
| memory.similarity-threshold | 0.5 | Similarity threshold |
| memory.embedding-model | text-embedding-v1 | Embedding model name |
| memory.summary-enabled | true | Enable auto summary |
| memory.async-embedding | true | Enable async vectorization |

### Qdrant Vector Database Configuration

| Item | Default | Description |
|--------|--------|------|
| qdrant.host | localhost | Qdrant service address (local deployment) |
| qdrant.port | 6333 | Qdrant HTTP port |
| qdrant.grpc-port | 6334 | Qdrant gRPC port |
| qdrant.collection-name | conversation_messages | Collection name |
| qdrant.vector-dimensions | 1024 | Vector dimensions |
| qdrant.distance | cosine | Similarity metric type |
| qdrant.index-type | hnsw | Index type |
| qdrant.hnsw-m | 16 | HNSW index parameter |
| qdrant.hnsw-ef-construct | 100 | HNSW construction parameter |
| qdrant.timeout-seconds | 30 | Connection timeout |
| qdrant.auto-create-collection | true | Auto create Collection |

### Local Deployment Instructions

**Recommended Deployment**: Docker single container deployment

**Component Dependencies**:
- Qdrant: Single container, no additional dependencies

**Resource Requirements**:
- CPU: 1 core (recommended 2 cores)
- Memory: 512MB (recommended 2GB)
- Disk: 10GB (recommended 30GB)

**Startup Command**:
```bash
# Start Qdrant
docker run -p 6333:6333 -p 6334:6334 -v $(pwd)/qdrant_storage:/qdrant/storage qdrant/qdrant:latest

# Or use docker-compose
docker-compose up -d
```

**Visual Management Interface**:
- URL: `http://localhost:6333/dashboard`

## API

### ConversationMemoryManager

| Method | Parameters | Return | Description |
|------|------|--------|------|
| addMessage | sessionId: String, role: String, content: String | void | Add new message |
| retrieveRelevantMessages | sessionId: String, query: String, topK: int, threshold: float | List\<ChatMessage\> | Semantic retrieval |
| buildContext | sessionId: String, query: String | String | Assemble context |
| generateSummary | sessionId: String | String | Manual trigger summary |
| clearMemory | sessionId: String | void | Clear memory |
| getMemoryStats | sessionId: String | MemoryStats | Get statistics |

### VectorStore

| Method | Parameters | Return | Description |
|------|------|--------|------|
| init | None | void | Initialize connection, create Collection and index |
| insert | messages: List\<ChatMessage\> | void | Insert message vectors |
| search | sessionId: String, queryVector: float[], topK: int, threshold: float | List\<ChatMessage\> | Semantic retrieval |
| deleteBySessionId | sessionId: String | void | Delete all messages for session |
| countBySessionId | sessionId: String | int | Count messages for session |
| isConnected | None | boolean | Check connection status |

### VectorService

| Method | Parameters | Return | Description |
|------|------|--------|------|
| embed | text: String | float[] | Text vectorization |
| batchEmbed | texts: List\<String\> | List\<float[]\> | Batch vectorization |