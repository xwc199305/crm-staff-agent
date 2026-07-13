# LLM-Based Knowledge Base Intelligent Matching

## Overview

The current system uses keyword-based knowledge base matching logic, matching knowledge base names and descriptions through predefined keyword lists. This approach has limitations: limited keyword coverage, inability to understand semantics, and difficulty coping with changes in knowledge base names or descriptions. To improve the accuracy and flexibility of knowledge base matching, it is necessary to implement LLM-based intelligent matching logic, enabling the system to understand the content domain based on knowledge base names and descriptions, and automatically select the most suitable knowledge base for user intent.

## Current Situation Analysis

### Current Issues

1. **Keyword matching limitations**: Current keyword matching can only match predefined keywords, cannot understand semantics
2. **Limited coverage**: Cannot match when knowledge base names or descriptions do not contain predefined keywords
3. **Lack of context understanding**: Cannot select the most suitable knowledge base based on specific content of user queries
4. **High maintenance cost**: Adding new intent types or knowledge bases requires manual maintenance of keyword mappings

## Goals

1. Implement LLM-based knowledge base intelligent matching functionality
2. Determine which knowledge base to retrieve based on intent type, user query, and knowledge base list information through LLM
3. Establish caching mechanism to avoid repeated LLM calls, reducing latency and costs
4. Keep existing matching chain as fallback solution

## Impact Scope

- `DifyKnowledgeBaseServiceImpl.java` - Implement LLM-based knowledge base matching logic
- `RagServiceImpl.java` - Extract generic LLM call methods for knowledge base matching
- `application.properties` - Add LLM matching related configuration items
- New `KnowledgeBaseMatcher.java` - Knowledge base matcher interface
- New `LLMKnowledgeBaseMatcher.java` - LLM knowledge base matcher implementation

## Risk Assessment

- Medium risk: Introducing LLM calls may increase latency, need to configure timeout and caching mechanisms
- Testing required: Ensure LLM matching accuracy and caching mechanism work correctly
- Configuration required: Need to configure LLM parameters (model, timeout, etc.)

## Success Criteria

1. Can determine which knowledge base to match for intent through LLM based on knowledge base name and description
2. LLM matching results can be cached to avoid repeated calls
3. Cache can be invalidated when knowledge base list is refreshed
4. Keep configuration mapping, keyword matching, and default dataset as fallback chain
5. LLM call timeout does not affect user queries

## Design

### Matching Chain Priority

1. **Configuration Mapping** (Highest priority): Static mapping configured via `intent.knowledge-base-mappings`
2. **LLM Matching** (Cached): Intent→knowledge base mapping determined by LLM (with caching)
3. **Keyword Matching**: Matching based on predefined keywords
4. **Default Dataset** (Fallback): Use default value configured via `dify.dataset-id`

### LLM Prompt Design

```
You are a knowledge base classification expert. Please select the most appropriate knowledge base from the following list based on the user's intent and query content.

Intent Type: {intentType}
User Query: {query}

Knowledge Base List:
{knowledgeBaseList}

Please return the most appropriate knowledge base ID. If no appropriate knowledge base exists, return "DEFAULT".

Return Format:
Knowledge Base ID: {datasetId}
```

### Cache Strategy

- Cache Key: `intentType`
- Cache Value: `datasetId`
- Cache Invalidation: When knowledge base list is refreshed
- Cache TTL: Same as knowledge base list refresh interval

### Timeout Mechanism

- LLM call timeout: 5 seconds
- Timeout handling: Degrade to keyword matching or default dataset