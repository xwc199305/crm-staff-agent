## ADDED Requirements

### Requirement: System can query Dify knowledge base
The system SHALL query the Dify knowledge base for answers to user questions.

#### Scenario: Successful knowledge base query
- **WHEN** a user question is submitted to the knowledge base service
- **THEN** system SHALL call Dify API to retrieve relevant knowledge
- **AND** return the answer from Dify knowledge base

#### Scenario: Knowledge base query with fallback
- **WHEN** Dify API is unavailable or returns an error
- **THEN** system SHALL fallback to local static knowledge
- **AND** return a reasonable response

#### Scenario: Configurable Dify connection
- **WHEN** the application starts
- **THEN** system SHALL read Dify configuration from application.properties
- **AND** use configured base URL, API key, and app ID

#### Scenario: Timeout handling
- **WHEN** Dify API response takes longer than configured timeout
- **THEN** system SHALL cancel the request
- **AND** fallback to local knowledge

### Requirement: Dify knowledge base service provides structured response
The system SHALL parse Dify API response and extract structured information.

#### Scenario: Extract answer from Dify response
- **WHEN** Dify API returns a successful response
- **THEN** system SHALL extract the answer content
- **AND** return it to the caller

#### Scenario: Handle empty response
- **WHEN** Dify API returns no relevant knowledge
- **THEN** system SHALL handle it gracefully
- **AND** fallback to local knowledge

### Requirement: Dify integration can be disabled via configuration
The system SHALL allow disabling Dify knowledge base integration through configuration.

#### Scenario: Disable Dify integration
- **WHEN** `dify.enabled=false` is set in configuration
- **THEN** system SHALL skip Dify knowledge base queries
- **AND** always use local static knowledge