## ADDED Requirements

### Requirement: ReAct mode API endpoint
The system SHALL provide a dedicated API endpoint for ReAct mode that allows users to interact with the true ReAct Agent.

#### Scenario: ReAct mode chat
- **WHEN** user sends a POST request to `/api/agent/chat-react` with a query
- **THEN** system processes the query using ReAct Agent with tool capabilities
- **AND** returns the Agent's response including any tool usage information

#### Scenario: ReAct mode with conversation context
- **WHEN** user sends multiple messages to `/api/agent/chat-react`
- **THEN** system maintains conversation context across messages
- **AND** Agent can reference previous messages in its reasoning

### Requirement: ReAct mode response includes tool usage metadata
The system SHALL return tool usage information in the API response for ReAct mode.

#### Scenario: Response includes tool usage
- **WHEN** Agent calls tools during processing
- **THEN** response includes tool usage details
- **AND** response includes reasoning steps if available

### Requirement: Existing API endpoints remain unchanged
The system SHALL ensure existing API endpoints continue to work as before.

#### Scenario: Existing chat endpoint unchanged
- **WHEN** user calls `/api/agent/chat`
- **THEN** system uses existing implementation
- **AND** behavior matches previous versions

#### Scenario: Existing chat-with-intent endpoint unchanged
- **WHEN** user calls `/api/agent/chat-with-intent`
- **THEN** system uses existing implementation
- **AND** behavior matches previous versions