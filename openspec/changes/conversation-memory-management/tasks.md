# Conversation Memory Management Implementation Tasks

## 1. Qdrant Dependencies and Configuration

- [ ] 1.1 Add Qdrant Java SDK dependency to pom.xml
- [ ] 1.2 Create `QdrantConfig` configuration class to read Qdrant connection parameters
- [ ] 1.3 Implement Qdrant client initialization and connection management
- [ ] 1.4 Add docker-compose.yml to start Qdrant container

## 2. Vector Database Operation Layer

- [ ] 2.1 Create `VectorStore` interface (init, insert, search, deleteBySessionId, countBySessionId, isConnected)
- [ ] 2.2 Create `QdrantVectorStore` implementation class
- [ ] 2.3 Implement Collection creation logic (with HNSW index)
- [ ] 2.4 Implement Payload index building logic (session_id, timestamp)
- [ ] 2.5 Implement vector insertion operation (upsert)
- [ ] 2.6 Implement semantic retrieval operation (with session_id filtering and similarity threshold)
- [ ] 2.7 Implement session message deletion operation
- [ ] 2.8 Implement connection status check and degradation mechanism

## 3. Data Structure Extension

- [ ] 3.1 Create `ChatMessage` class (id, role, content, timestamp, vector, tokenCount)
- [ ] 3.2 Create `MemoryStats` class (sessionId, messageCount, tokenCount, hasSummary, vectorCount)
- [ ] 3.3 Extend `ConversationContext` class (add messages, summary, tokenCount, remove messageVectors)

## 4. Vector Service Implementation

- [ ] 4.1 Create `VectorService` interface (embed, batchEmbed methods)
- [ ] 4.2 Create `DashScopeVectorServiceImpl` implementation class, call DashScope Embedding API
- [ ] 4.3 Add vector caching mechanism (reuse vectors for identical text)

## 5. Summary Generator Implementation

- [ ] 5.1 Create `SummaryGenerator` class
- [ ] 5.2 Implement LLM-based summary generation logic
- [ ] 5.3 Implement Token counting logic

## 6. ConversationMemoryManager Implementation

- [ ] 6.1 Create `ConversationMemoryManager` class
- [ ] 6.2 Implement `addMessage` method (vectorization + Qdrant insertion)
- [ ] 6.3 Implement `retrieveRelevantMessages` method (call Qdrant retrieval)
- [ ] 6.4 Implement `buildContext` method (assemble context)
- [ ] 6.5 Implement `generateSummary` method (manual trigger summary)
- [ ] 6.6 Implement auto-summary trigger mechanism (based on Token threshold)
- [ ] 6.7 Implement `clearMemory` and `getMemoryStats` methods

## 7. Service Layer Integration

- [ ] 7.1 Modify `ReactAgentServiceImpl.chatWithIntent`, integrate Memory management
- [ ] 7.2 Modify `ReactAgentWithToolsServiceImpl.chatWithIntent`, integrate Memory management
- [ ] 7.3 Use retrieved relevant historical context in ReAct loop

## 8. Configuration and Testing

- [ ] 8.1 Add Memory and Qdrant configuration items in `application.properties`
- [ ] 8.2 Compilation verification
- [ ] 8.3 Start Qdrant container
- [ ] 8.4 Application startup verification
- [ ] 8.5 API interface testing (verify semantic retrieval and summary generation)

## 9. Cleanup and Optimization

- [ ] 9.1 Clean up expired session Memory (delete expired data in Qdrant)
- [ ] 9.2 Add Memory statistics logging
- [ ] 9.3 Optimize Qdrant query performance (adjust HNSW parameters)
- [ ] 9.4 Implement Qdrant batch insertion optimization