package com.example.staffagent.intent.impl;

import com.example.staffagent.dto.IntentResult;
import com.example.staffagent.intent.IntentRecognizer;
import com.example.staffagent.intent.IntentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentRecognizerImpl implements IntentRecognizer {

    private final RuleBasedMatcher ruleBasedMatcher;
    private final LLMIntentRecognizer llmIntentRecognizer;

    @Value("${intent.confidence-threshold:0.7}")
    private double confidenceThreshold;

    @Value("${intent.llm-confidence-threshold:0.5}")
    private double llmConfidenceThreshold;

    @Override
    public IntentResult recognize(String query) {
        log.debug("Recognizing intent for query: {}", query);

        IntentResult ruleResult = ruleBasedMatcher.match(query);
        if (ruleResult != null) {
            log.info("Rule-based matched intent: {} (confidence: {})",
                    ruleResult.getIntentType(), ruleResult.getConfidence());
            return ruleResult;
        }

        log.debug("No rule-based match found, falling back to LLM");

        IntentResult llmResult = llmIntentRecognizer.recognize(query);
        if (llmResult != null && llmResult.getConfidence() >= llmConfidenceThreshold) {
            log.info("LLM matched intent: {} (confidence: {})",
                    llmResult.getIntentType(), llmResult.getConfidence());
            return llmResult;
        }

        log.info("No intent matched, returning UNKNOWN");
        return IntentResult.unknown(query);
    }
}