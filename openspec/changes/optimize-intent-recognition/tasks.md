# Optimize Intent Recognition - Task List

## Phase 1: Fix extractField Method

### Task 1.1 Fix extractField Bug
- File: `src/main/java/com/example/staffagent/intent/impl/LLMIntentRecognizer.java`
- Content: Modify line 133, use `end` variable for field extraction
- Status: ✅ Completed

## Phase 2: Add LLM Confidence Threshold

### Task 2.1 Add Configuration Items
- File: `src/main/resources/application.properties`
- Content: Add `intent.llm-confidence-threshold=0.5`
- Status: ✅ Completed

### Task 2.2 Update IntentRecognizerImpl
- File: `src/main/java/com/example/staffagent/intent/impl/IntentRecognizerImpl.java`
- Content:
  - Add `llmConfidenceThreshold` field
  - Modify recognize method, use separate LLM threshold
- Status: ✅ Completed

## Phase 3: Optimize UNKNOWN Intent Handling

### Task 3.1 Update ReactAgentServiceImpl
- File: `src/main/java/com/example/staffagent/service/impl/ReactAgentServiceImpl.java`
- Content: Add explicit UNKNOWN intent handling branch, return fallback message directly
- Status: ✅ Completed

## Phase 4: Testing and Verification

### Task 4.1 Compilation Verification
- Command: `mvn clean compile`
- Status: ✅ Completed

### Task 4.2 Run Tests
- Test rule-based matching: queries containing keywords (e.g., "my order")
- Test LLM recognition: queries not matching rules but having intent (e.g., "I want to know about aftersales service")
- Test UNKNOWN: unrelated queries (e.g., "how's the weather")
- Status: ✅ Completed

## Phase 5: Documentation

### Task 5.1 Create openspec Documents
- Files:
  - `proposal.md`
  - `design.md`
  - `specs/LLMIntentRecognizer.md`
  - `specs/IntentRecognizerImpl.md`
  - `tasks.md`
- Status: ✅ Completed