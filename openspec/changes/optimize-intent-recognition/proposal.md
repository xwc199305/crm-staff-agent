# Optimize Intent Recognition Capability

## Overview

Current intent recognition has two issues:
1. The `extractField` method in `LLMIntentRecognizer` has a bug - calculates the `end` variable but doesn't use it, causing field extraction errors
2. `IntentRecognizerImpl` uses a unified confidence threshold (0.7), and LLM results are discarded due to confidence below threshold, always returning UNKNOWN
3. UNKNOWN intent handling logic is not clear enough, needs to clearly distinguish between UNKNOWN and no handler cases

## Current Situation Analysis

### Issue 1: extractField Method Bug

```java
private String extractField(String response, String prefix) {
    int start = response.indexOf(prefix);
    if (start == -1) {
        return null;
    }
    int end = response.indexOf("\n", start);  // calculates end
    if (end == -1) {
        end = response.length();
    }
    return response.substring(start + prefix.length()).trim();  // but doesn't use end
}
```

When LLM returns multi-line results, field extraction includes everything from prefix to end of string, causing parsing failure.

### Issue 2: Unified Confidence Threshold

`IntentRecognizerImpl` uses `intent.confidence-threshold=0.7` as a unified threshold for rule matching and LLM recognition. LLM intent recognition confidence is typically lower than rule matching, causing LLM recognition results to be discarded.

### Issue 3: UNKNOWN Intent Handling

When intent is recognized as UNKNOWN, the handling logic is not clear. Need to directly return fallback text and stop executing subsequent processes.

## Goals

1. Fix `extractField` method, correctly use `end` variable for field extraction
2. Add separate LLM confidence threshold configuration (default 0.5)
3. Optimize UNKNOWN intent handling logic, directly return fallback text

## Impact Scope

- `LLMIntentRecognizer.java` - Fix extractField method
- `IntentRecognizerImpl.java` - Add LLM confidence threshold
- `ReactAgentServiceImpl.java` - Optimize UNKNOWN intent handling
- `application.properties` - Add configuration items

## Risk Assessment

- Low risk: Bug fixes and logic optimization won't affect existing functionality
- Testing required: Ensure LLM intent recognition works correctly after fix

## Success Criteria

1. Valid intent results returned by LLM can be correctly parsed and used
2. UNKNOWN intent directly returns fallback text
3. Configuration items take effect correctly