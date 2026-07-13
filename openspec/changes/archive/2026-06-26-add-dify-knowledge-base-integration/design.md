## Context

The current project is an e-commerce customer service Agent with intent recognition and four core intent handlers. Each intent handler uses hardcoded static knowledge bases (HashMap) to answer user questions. This approach has the following problems:
- Knowledge content cannot be dynamically updated; each update requires code modification and redeployment
- Knowledge coverage is limited and cannot contain extensive detailed product information
- Lack of semantic understanding ability, only keyword exact matching is supported

Dify is an open-source LLM application development platform that provides knowledge base functionality. Documents can be uploaded to the knowledge base for semantic retrieval. Integrating Dify knowledge base can solve the above problems.

## Goals / Non-Goals

**Goals:**
- Implement Dify knowledge base API client wrapper
- Provide unified knowledge base query service interface
- Integrate four intent handlers with Dify knowledge base
- Support fallback to local static knowledge base when knowledge base query fails
- Provide Dify API configuration items for flexible configuration

**Non-Goals:**
- Do not implement Dify knowledge base management functions (such as document upload, deletion, etc.)
- Do not implement Dify application creation and management
- Do not involve Dify platform installation and deployment

## Decisions

### Decision 1: HTTP Client Selection

**Choice:** Use Spring WebClient

**Rationale:**
- WebClient is a reactive HTTP client provided by Spring WebFlux
- Non-blocking calls, better performance
- Consistent with the project's reactive style (project uses Reactor Mono)
- Supports advanced features like timeout, retry, etc.

**Alternatives Considered:**
- RestTemplate: Synchronous blocking, not recommended for new code
- OkHttp: Requires additional dependencies, WebClient is sufficient

### Decision 2: Dify API Calling Method

**Choice:** Use Dify Completion API (POST /v1/chat-messages)

**Rationale:**
- Completion API supports knowledge base retrieval and conversation history
- Returns structured response containing answer content and cited sources
- Supports streaming response, suitable for real-time chat scenarios

**API Endpoint:** `POST {dify-base-url}/v1/chat-messages`

**Request Parameters:**
- `inputs`: Input parameters (optional)
- `query`: User query
- `response_mode`: Response mode (streaming or block)
- `user`: User identifier (for conversation history)

### Decision 3: Knowledge Base Query vs Local Knowledge Priority

**Choice:** Dify knowledge base first, local knowledge as fallback

**Rationale:**
- Dify knowledge base content is more comprehensive and updated more timely
- Local static knowledge can serve as a degradation solution, still providing basic answers when Dify service is unavailable
- Hybrid mode balances flexibility and reliability

**Process:**
```
User Query → Call Dify Knowledge Base → Get Answer
              ↓ Query Failed/Timeout
         Use Local Static Knowledge Base → Get Answer
```

### Decision 4: Intent Handler Integration Approach

**Choice:** Unified integration through DifyKnowledgeBaseService

**Rationale:**
- Inject DifyKnowledgeBaseService into each intent handler
- Handler first calls knowledge base query, falls back to local knowledge on failure
- Keep handler interface unchanged, reduce coupling

**Architecture:**
```
IntentHandler → DifyKnowledgeBaseService → DifyClient → Dify API
                    ↓ Fallback
                Local Knowledge Map
```

## Risks / Trade-offs

### Risk 1: Dify Service Unavailability
**Risk:** Dify service downtime or network failure causing knowledge base query failure
**Mitigation:**
- Set reasonable timeout
- Implement fallback mechanism, use local static knowledge base when query fails
- Add retry mechanism

### Risk 2: API Key Leakage
**Risk:** Dify API Key stored in configuration file may be leaked
**Mitigation:**
- Use environment variables to configure API Key
- Do not print API Key in logs
- Limit API Key permission scope

### Risk 3: Knowledge Base Query Latency
**Risk:** Network requests may cause response delay
**Mitigation:**
- Set reasonable timeout (e.g., 10 seconds)
- Use asynchronous calls to avoid blocking main thread
- Add caching mechanism (optional)

### Risk 4: Knowledge Base Answer Quality Instability
**Risk:** Answers returned by Dify may not meet expectations
**Mitigation:**
- Optimize knowledge base configuration and prompts on Dify platform
- Add answer quality evaluation (optional)
- Provide feedback mechanism (optional)

## Migration Plan

### Phase 1: Create Dify Client and Service
1. Create DifyResponse DTO
2. Create DifyClient wrapper for API calls
3. Create DifyKnowledgeBaseService to provide query interface

### Phase 2: Update Intent Handlers
1. Modify ProductConsultationHandler to integrate knowledge base query
2. Modify WarrantyPolicyHandler to integrate knowledge base query
3. Modify AftersalesProcessHandler to integrate knowledge base query
4. Modify OrderInquiryHandler to integrate knowledge base query

### Phase 3: Configuration Updates
1. Update application.properties to add Dify configuration items
2. Add environment variable support

### Phase 4: Verification Testing
1. Compilation verification
2. Startup verification
3. Test knowledge base query functionality
4. Test fallback mechanism

### Rollback Strategy
- If Dify integration encounters issues, knowledge base query can be disabled via configuration
- Use configuration item `dify.enabled=false` to disable knowledge base query
- Fall back to pure local knowledge mode

## Open Questions

**Q1: Do we need to add caching mechanism?**
- Suggestion: For frequently queried questions, add local caching
- Consider in subsequent iterations

**Q2: Do we need to support multiple Dify knowledge bases?**
- Suggestion: Currently only supports one knowledge base, can be extended later

**Q3: Do we need streaming response support?**
- Suggestion: Currently uses block mode, streaming response can be a future optimization