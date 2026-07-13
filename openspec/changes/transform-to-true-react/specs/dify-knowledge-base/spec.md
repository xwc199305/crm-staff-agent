## ADDED Requirements

### Requirement: Knowledge base service supports tool call mode
The system SHALL provide a method that can be directly registered as an Agent tool for knowledge base queries.

#### Scenario: Knowledge base as Agent tool
- **WHEN** ReAct Agent needs product information
- **THEN** Agent calls the knowledge base tool method
- **AND** method returns formatted knowledge base results suitable for LLM processing

#### Scenario: Tool method with single query parameter
- **WHEN** Agent invokes the knowledge base tool
- **THEN** tool accepts a single query string parameter
- **AND** tool handles intent matching internally to select appropriate dataset

#### Scenario: Tool returns concise results
- **WHEN** knowledge base tool is called
- **THEN** tool returns relevant document content without RAG generation
- **AND** Agent uses the content to generate final response

