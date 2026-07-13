# Optimize Intent Recognition Capability - Detailed Design

## Architecture Design

### Intent Recognition Flow

```
User Input -> RuleBasedMatcher -> Match Success -> Return IntentResult
                              -> Match Failure -> LLMIntentRecognizer -> Confidence >= Threshold -> Return IntentResult
                                                              -> Confidence < Threshold  -> Return UNKNOWN
```

### UNKNOWN Intent Handling

```
IntentResult(intentType=UNKNOWN) -> Directly return DEFAULT_REPLY
```

## Component Responsibilities

### LLMIntentRecognizer

Responsible for calling large model for intent recognition and parsing return results.

**Fixes:**
- `extractField` method: Correctly use `end` variable for field extraction

### IntentRecognizerImpl

Responsible for integrating rule matching and LLM recognition, returning final intent result.

**Optimizations:**
- Add `llmConfidenceThreshold` configuration item (default 0.5)
- Use separate threshold to judge LLM recognition results

### ReactAgentServiceImpl

Responsible for coordinating intent recognition and intent handling.

**Optimizations:**
- Clear UNKNOWN intent handling branch
- Distinguish between UNKNOWN and no handler cases

## Data Structures

### IntentResult

```java
public class IntentResult {
    private IntentType intentType;      // Intent type
    private Double confidence;          // Confidence 0.0-1.0
    private List<String> keywords;      // Keywords
    private Map<String, String> params; // Parameters
    private String originalQuery;       // Original query
    
    public static IntentResult unknown(String query) {
        return IntentResult.builder()
                .intentType(IntentType.UNKNOWN)
                .confidence(0.0)
                .originalQuery(query)
                .build();
    }
}
```

## Configuration Items

| Item | Default | Description |
|--------|--------|------|
| `intent.confidence-threshold` | 0.7 | Rule matching confidence threshold |
| `intent.llm-confidence-threshold` | 0.5 | LLM intent recognition confidence threshold |

## API Changes

No API changes, only internal logic optimization.

## Error Handling

- LLM call failure: Return null, handled by IntentRecognizerImpl
- Field parsing failure: Return null, handled by IntentRecognizerImpl
- UNKNOWN intent: Directly return fallback text