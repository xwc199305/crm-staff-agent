## ADDED Requirements

### Requirement: System can provide order status information
The system SHALL allow users to inquire about order status, shipping information, and delivery details.

#### Scenario: Query order status by order number
- **WHEN** user provides an order number
- **THEN** system SHALL return the current order status
- **AND** include shipping and tracking information if available

#### Scenario: Query recent orders
- **WHEN** user asks about recent orders without providing an order number
- **THEN** system SHALL prompt the user for their order number or registered phone number
- **AND** offer to look up recent orders

#### Scenario: Provide shipping information
- **WHEN** user asks about shipping or delivery
- **THEN** system SHALL provide shipping carrier information
- **AND** include estimated delivery date

#### Scenario: Handle invalid order number
- **WHEN** user provides an invalid or non-existent order number
- **THEN** system SHALL inform the user that the order was not found
- **AND** suggest checking the order number and trying again

### Requirement: Order inquiry handler follows security best practices
The system SHALL protect user privacy and order information.

#### Scenario: Verify order ownership
- **WHEN** user queries an order
- **THEN** system SHALL verify the user has permission to access the order
- **AND** require additional verification if necessary

#### Scenario: Avoid exposing sensitive information
- **WHEN** providing order information
- **THEN** system SHALL avoid exposing full payment details
- **AND** mask sensitive information appropriately