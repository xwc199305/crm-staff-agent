# Dynamic Knowledge Base List Retrieval

## Overview

The current system uses a fixed knowledge base ID (configured via `dify.dataset-id`), unable to dynamically select different knowledge bases for retrieval based on user intent. To improve the flexibility of RAG retrieval capability, it is necessary to implement dynamic knowledge base list retrieval functionality and match the most suitable knowledge base based on intent type during intent recognition.

## Current Situation Analysis

### Current Issues

1. **Fixed knowledge base configuration**: System only supports configuring one knowledge base ID, all intent queries use the same knowledge base
2. **Lack of knowledge base selection mechanism**: Cannot select different domain knowledge bases for retrieval based on different user intents
3. **Poor scalability**: Adding new knowledge bases requires modifying configuration and restarting the service

## Goals

1. Implement API call for dynamically retrieving Dify knowledge base list
2. Establish mapping relationship between intent types and knowledge bases
3. Select appropriate knowledge base for retrieval based on intent type during intent recognition
4. Support caching knowledge base list to reduce API call frequency

## Impact Scope

- `DifyFeignClient.java` - Add interface for retrieving knowledge base list
- `DifyClient.java` - Add method for retrieving knowledge base list
- `DifyKnowledgeBaseService.java` - Add dynamic retrieval method
- `DifyKnowledgeBaseServiceImpl.java` - Implement dynamic retrieval logic
- `IntentHandler` implementation classes - Update to use dynamic knowledge base selection
- `application.properties` - Add relevant configuration items
- New `KnowledgeBaseInfo.java` - Knowledge base information DTO

## Risk Assessment

- Low risk: New feature, does not affect existing logic
- Testing required: Ensure knowledge base list retrieval and dynamic selection work correctly
- Configuration required: Need to configure intent-knowledge base mapping relationships

## Success Criteria

1. Can successfully retrieve Dify knowledge base list
2. Can automatically select appropriate knowledge base based on intent type
3. Supports caching mechanism to reduce API calls
4. Configuration changes do not require service restart