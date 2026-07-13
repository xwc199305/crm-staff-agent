# LLMIntentRecognizer Specification

## Interface Definition

```java
@Component
public class LLMIntentRecognizer {

    public IntentResult recognize(String query);
}
```

## Method Description

### recognize(String query)

Call large model for intent recognition.

**Parameters**:
- `query`: User input text

**Return Value**:
- `IntentResult`: Intent recognition result containing intent type, confidence, keywords, etc.
- `null`: LLM call failed or API key not configured

## Internal Methods

### extractField(String response, String prefix)

Extract specified field from LLM response.

**Before Fix**:
```java
return response.substring(start + prefix.length()).trim();
```

**After Fix**:
```java
return response.substring(start + prefix.length(), end).trim();
```

### parseIntentType(String intentStr)

Parse intent type string.

**Return Value**:
- Matched IntentType (PRODUCT_CONSULTATION, WARRANTY_POLICY, AFTERSALES_PROCESS, ORDER_INQUIRY)
- IntentType.UNKNOWN: Cannot match

### parseConfidence(String confidenceStr)

Parse confidence string.

**Return Value**:
- Confidence value between 0.0-1.0
- Default 0.5: Parse failed

## Format Requirements

LLM response must contain the following fields:

```
INTENT: <intent type>
CONFIDENCE: <0.0-1.0>
KEYWORDS: <comma-separated keywords>
```

## Dependencies

- `DashScopeChatModel` - Large model client
- `ReActAgent` - Agent wrapper