## ADDED Requirements

### Requirement: System can recognize ecommerce customer service intents
The system SHALL recognize and classify user inputs into predefined ecommerce customer service intent categories.

#### Scenario: Recognize product usage consultation intent
- **WHEN** user input contains keywords like "how to use", "usage method", "operation guide", "function", "settings", "how to set", "usage instructions", "features"
- **THEN** system SHALL classify the intent as `PRODUCT_CONSULTATION`
- **AND** return a confidence score >= 0.7

#### Scenario: Recognize warranty policy intent
- **WHEN** user input contains keywords like "warranty", "guarantee", "warranty period", "warranty policy", "warranty terms"
- **THEN** system SHALL classify the intent as `WARRANTY_POLICY`
- **AND** return a confidence score >= 0.7

#### Scenario: Recognize aftersales process intent
- **WHEN** user input contains keywords like "return", "refund", "exchange", "aftersales", "repair"
- **THEN** system SHALL classify the intent as `AFTERSALES_PROCESS`
- **AND** return a confidence score >= 0.7

#### Scenario: Recognize order inquiry intent
- **WHEN** user input contains keywords like "order", "logistics", "shipping", "delivery", "order status", "track order"
- **THEN** system SHALL classify the intent as `ORDER_INQUIRY`
- **AND** return a confidence score >= 0.7

#### Scenario: Return unrecognized intent
- **WHEN** user input does not match any predefined intent patterns
- **THEN** system SHALL classify the intent as `UNKNOWN`
- **AND** return a confidence score < 0.7

#### Scenario: Provide structured intent result
- **WHEN** intent recognition is performed
- **THEN** system SHALL return an IntentResult containing:
  - IntentType (PRODUCT_CONSULTATION, WARRANTY_POLICY, AFTERSALES_PROCESS, ORDER_INQUIRY, UNKNOWN)
  - Confidence score (0.0 - 1.0)
  - Matched keywords
  - Extracted parameters
  - Original query

### Requirement: Intent recognition uses hybrid approach
The system SHALL use a hybrid approach combining rule-based matching and LLM semantic understanding for intent recognition.

#### Scenario: Rule-based matching takes priority
- **WHEN** user input matches a predefined rule pattern
- **THEN** system SHALL use rule-based matching first
- **AND** skip LLM-based recognition to improve performance

#### Scenario: Fallback to LLM recognition
- **WHEN** user input does not match any rule patterns
- **THEN** system SHALL fallback to LLM semantic understanding
- **AND** use the LLM to classify the intent

### Requirement: Intent recognition supports configurable confidence threshold
The system SHALL allow configuration of the minimum confidence threshold for intent classification.

#### Scenario: Low confidence triggers fallback
- **WHEN** intent recognition confidence is below the configured threshold
- **THEN** system SHALL classify the intent as `UNKNOWN`
- **AND** route the request to the generic agent handler

#### Scenario: Configurable threshold
- **WHEN** the confidence threshold is configured in application.properties
- **THEN** system SHALL use the configured threshold value
- **AND** default to 0.7 if not configured