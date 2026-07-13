# Optimize Intent Recognition and Tool Call Logic

## Overview

The current system's intent recognition and tool call logic has the following issues: After intent recognition, it directly calls the knowledge base without a clear tool call decision mechanism; knowledge base matching uses caching, making it unable to reflect knowledge base list changes in real-time; the mapping cache between intent enum values and datasetId increases system complexity unnecessarily. This optimization will refactor intent recognition and tool call logic, establish a clear tool call decision mechanism, remove unnecessary caching, and reserve MCP interface call capability.

## Current Situation Analysis

### Current Issues

1. **Lack of tool call decision mechanism**: After intent recognition, it directly calls the knowledge base without judging which tool to call based on intent type
2. **Unreasonable caching mechanism**: Knowledge base matching uses caching, unable to reflect knowledge base list changes in real-time
3. **Redundant intent-dataset cache**: Mapping cache between intent enum values and datasetId increases system complexity
4. **MCP interface not reserved**: Intent types requiring external interface calls (like order inquiry) have no extension capability reserved

## Goals

1. Establish tool call decision mechanism, judge which tool to call based on intent type
2. Remove knowledge base matching caching mechanism, match knowledge base list in real-time each time
3. Remove intent enum value and datasetId caching mechanism
4. Reserve MCP interface call capability for order inquiry and other intents

## Impact Scope

- `IntentType.java` - Add tool type identification
- `IntentHandler` interface - Add tool call method
- `IntentHandlerFactory.java` - Update tool call logic
- `DifyKnowledgeBaseServiceImpl.java` - Remove caching mechanism
- `ProductConsultationHandler.java` - Update tool call method
- `OrderInquiryHandler.java` - Reserve MCP interface call
- New `ToolType.java` - Tool type enum
- New `ToolCallService.java` - Tool call service interface

## Risk Assessment

- Medium risk: Refactoring tool call logic, need to ensure existing functionality is not affected
- Testing required: Ensure tool call decisions are correct, knowledge base matching is real-time
- Configuration required: No additional configuration needed

## Success Criteria

1. Can judge which tool to call based on intent type
2. Product consultation intent can match knowledge base list in real-time (no cache)
3. Order inquiry intent has MCP interface call capability reserved
4. All unnecessary caching mechanisms are removed