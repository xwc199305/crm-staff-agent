## ADDED Requirements

### Requirement: SSE streaming chat endpoint
The system SHALL provide a streaming chat endpoint `/api/agent/chat-stream` that returns responses as Server-Sent Events (SSE) with `text/event-stream` content type.

#### Scenario: Simple streaming chat
- **WHEN** user sends a POST request to `/api/agent/chat-stream` with a message
- **THEN** system SHALL return a `text/event-stream` response
- **AND** stream `delta` events containing text fragments of the LLM response
- **AND** stream a `done` event with the complete reply when generation finishes

#### Scenario: Streaming chat with intent
- **WHEN** user sends a POST request to `/api/agent/chat-with-intent-stream` with userId, message, and sessionId
- **THEN** system SHALL first stream a `metadata` event containing intentType and confidence
- **AND** then stream `delta` events with LLM-generated text fragments
- **AND** finally stream a `done` event with the complete reply

#### Scenario: Default user and session handling
- **WHEN** userId is null or empty in the streaming request
- **THEN** system SHALL use "default-user" as userId
- **WHEN** sessionId is null or empty in the streaming request
- **THEN** system SHALL use "default" as sessionId

### Requirement: Streaming LLM generation via AgentScope SDK
The system SHALL use AgentScope Java SDK's native `ReActAgent.stream()` method to obtain token-by-token responses for the streaming endpoints.

#### Scenario: Successful streaming generation
- **WHEN** the prompt is built and sent to `ReActAgent.stream()` with `StreamOptions`
- **THEN** system SHALL receive `Flux<Event>` from the SDK
- **AND** filter events by `EventType.AGENT_RESULT` to extract final reply chunks
- **AND** emit each text fragment as a `delta` event to the client

#### Scenario: SDK streaming error
- **WHEN** the SDK `stream()` produces an error or the LLM is unreachable
- **THEN** system SHALL emit a `done` event with an error message
- **AND** close the stream gracefully

#### Scenario: Empty content events
- **WHEN** an `AGENT_RESULT` event has null or empty text content
- **THEN** system SHALL skip that event without emitting a `delta`

### Requirement: Conversation memory persistence after streaming
The system SHALL persist the complete conversation (user message + full assistant reply) to memory after the streaming completes successfully.

#### Scenario: Memory written on stream completion
- **WHEN** the LLM stream completes without errors
- **THEN** system SHALL accumulate all delta fragments into a complete reply
- **AND** write the user message and assistant reply to ConversationMemoryManager via addMessagePair

#### Scenario: Memory not written on stream cancellation
- **WHEN** the client disconnects before the stream completes
- **THEN** system SHALL NOT write incomplete memory entries
- **AND** release ThreadLocal context if set

### Requirement: ThreadLocal context propagation in streaming
The system SHALL set ConversationContextHolder context before streaming begins and clear it after streaming ends (including cancellation).

#### Scenario: Context available during streaming generation
- **WHEN** the streaming endpoint processes a chat-with-intent request
- **THEN** system SHALL build context from ConversationMemoryManager and set it in ConversationContextHolder
- **AND** StreamingChatService SHALL be able to read context from ConversationContextHolder during generation

#### Scenario: Context cleared on completion
- **WHEN** the stream completes or is cancelled
- **THEN** system SHALL call ConversationContextHolder.clearContext()

### Requirement: Existing non-streaming endpoints remain unchanged
The system SHALL ensure all existing non-streaming API endpoints continue to function exactly as before.

#### Scenario: Non-streaming chat-with-intent unaffected
- **WHEN** user calls `/api/agent/chat-with-intent`
- **THEN** system returns a synchronous `ApiResponse<ChatResponse>` as before
- **AND** behavior matches the pre-streaming implementation

#### Scenario: Non-streaming chat unaffected
- **WHEN** user calls `/api/agent/chat`
- **THEN** system returns a synchronous `ApiResponse<String>` as before
