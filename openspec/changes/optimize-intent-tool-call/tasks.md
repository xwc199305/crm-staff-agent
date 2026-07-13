# Optimize Intent Recognition and Tool Call Logic - Task List

## Phase 1: Create Tool Type Enum

### Task 1.1 Create ToolType Enum
- File: `src/main/java/com/example/staffagent/tool/ToolType.java`
- Status: Pending

## Phase 2: Update Intent Type Enum

### Task 2.1 Update IntentType Enum, Add Tool Type Association
- File: `src/main/java/com/example/staffagent/intent/IntentType.java`
- Status: Pending

## Phase 3: Create Tool Call Service

### Task 3.1 Create ToolCallService Interface
- File: `src/main/java/com/example/staffagent/tool/ToolCallService.java`
- Status: Pending

### Task 3.2 Create ToolCallServiceImpl Implementation Class
- File: `src/main/java/com/example/staffagent/tool/impl/ToolCallServiceImpl.java`
- Status: Pending

## Phase 4: Update Knowledge Base Service

### Task 4.1 Remove Cache Mechanism, Implement Real-Time Matching
- File: `src/main/java/com/example/staffagent/dify/impl/DifyKnowledgeBaseServiceImpl.java`
- Status: Pending

## Phase 5: Update Intent Handler

### Task 5.1 Update IntentHandler Interface, Add Tool Type Get Method
- File: `src/main/java/com/example/staffagent/handler/IntentHandler.java`
- Status: Pending

### Task 5.2 Update IntentHandlerFactory, Implement Tool Call Decision
- File: `src/main/java/com/example/staffagent/handler/impl/IntentHandlerFactory.java`
- Status: Pending

### Task 5.3 Update ProductConsultationHandler
- File: `src/main/java/com/example/staffagent/handler/impl/ProductConsultationHandler.java`
- Status: Pending

### Task 5.4 Update WarrantyPolicyHandler
- File: `src/main/java/com/example/staffagent/handler/impl/WarrantyPolicyHandler.java`
- Status: Pending

### Task 5.5 Update AftersalesProcessHandler
- File: `src/main/java/com/example/staffagent/handler/impl/AftersalesProcessHandler.java`
- Status: Pending

### Task 5.6 Update OrderInquiryHandler, Reserve MCP Interface
- File: `src/main/java/com/example/staffagent/handler/impl/OrderInquiryHandler.java`
- Status: Pending

## Phase 6: Update Configuration

### Task 6.1 Remove intent.knowledge-base-mappings Configuration Item
- File: `src/main/resources/application.properties`
- Status: Pending

## Phase 7: Compilation and Testing

### Task 7.1 Compilation Verification
- Command: `mvn clean compile`
- Status: Pending

### Task 7.2 Startup Testing
- Test tool call decision
- Test knowledge base real-time matching
- Test MCP interface reservation
- Status: Pending