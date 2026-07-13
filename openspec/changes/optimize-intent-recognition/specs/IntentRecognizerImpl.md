# IntentRecognizerImpl Specification

## Interface Definition

```java
@Service
public class IntentRecognizerImpl implements IntentRecognizer {

    public IntentResult recognize(String query);
}
```

## Method Description

### recognize(String query)

Integrates rule-based matching and LLM recognition, returns final intent result.

**Process**:
1. Call `RuleBasedMatcher.match(query)` for rule-based matching
2. If rule-based matching succeeds, return result directly
3. If rule-based matching fails, call `LLMIntentRecognizer.recognize(query)` for LLM recognition
4. If LLM recognition confidence >= `llmConfidenceThreshold`, return result
5. Otherwise return UNKNOWN

**Parameters**:
- `query`: User input text

**Return Value**:
- `IntentResult`: Intent recognition result
- IntentResult.unknown(query): Cannot recognize

## Configuration Items

| Configuration Item | Default Value | Description |
|--------------------|---------------|-------------|
| `intent.confidence-threshold` | 0.7 | Rule-based confidence threshold (deprecated, kept for compatibility) |
| `intent.llm-confidence-threshold` | 0.5 | LLM intent recognition confidence threshold |

## Dependencies

- `RuleBasedMatcher` - Rule-based matcher
- `LLMIntentRecognizer` - LLM intent recognizer

## Log Specification

| Scenario | Log Level | Format |
|----------|-----------|--------|
| Rule-based matching succeeded | INFO | `Rule-based matched intent: {intent} (confidence: {confidence})` |
| Rule-based matching failed | DEBUG | `No rule-based match found, falling back to LLM` |
| LLM recognition succeeded | INFO | `LLM matched intent: {intent} (confidence: {confidence})` |
| Cannot recognize | INFO | `No intent matched, returning UNKNOWN` |