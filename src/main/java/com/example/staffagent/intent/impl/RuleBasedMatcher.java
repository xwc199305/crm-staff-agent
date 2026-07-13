package com.example.staffagent.intent.impl;

import com.example.staffagent.dto.IntentResult;
import com.example.staffagent.intent.IntentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RuleBasedMatcher {

    private static final Pattern PRODUCT_CONSULTATION_PATTERN = Pattern.compile(
            "(how to use|usage method|operation guide|function|settings|how to set|usage instructions|features|parameters|specifications)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern WARRANTY_POLICY_PATTERN = Pattern.compile(
            "(warranty|guarantee|warranty period|warranty policy|warranty terms|warranty coverage|free warranty)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern AFTERSALES_PROCESS_PATTERN = Pattern.compile(
            "(return|refund|exchange|aftersales|repair|service process|refund process|return process)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ORDER_INQUIRY_PATTERN = Pattern.compile(
            "(order|logistics|shipping|delivery|order status|track order|shipping info|tracking number|tracking id)",
            Pattern.CASE_INSENSITIVE
    );

    public IntentResult match(String query) {
        if (query == null || query.trim().isEmpty()) {
            return IntentResult.unknown(query);
        }

        List<String> matchedKeywords = new ArrayList<>();
        IntentType intentType;
        double confidence;

        if (matchesPattern(PRODUCT_CONSULTATION_PATTERN, query, matchedKeywords)) {
            intentType = IntentType.PRODUCT_CONSULTATION;
            confidence = 0.85;
        } else if (matchesPattern(WARRANTY_POLICY_PATTERN, query, matchedKeywords)) {
            intentType = IntentType.WARRANTY_POLICY;
            confidence = 0.85;
        } else if (matchesPattern(AFTERSALES_PROCESS_PATTERN, query, matchedKeywords)) {
            intentType = IntentType.AFTERSALES_PROCESS;
            confidence = 0.85;
        } else if (matchesPattern(ORDER_INQUIRY_PATTERN, query, matchedKeywords)) {
            intentType = IntentType.ORDER_INQUIRY;
            confidence = 0.85;
        } else {
            return null;
        }

        log.debug("Rule-based matching succeeded: intent={}, keywords={}", intentType, matchedKeywords);

        return IntentResult.builder()
                .intentType(intentType)
                .confidence(confidence)
                .keywords(matchedKeywords)
                .originalQuery(query)
                .build();
    }

    private boolean matchesPattern(Pattern pattern, String query, List<String> matchedKeywords) {
        Matcher matcher = pattern.matcher(query);
        boolean matched = false;
        while (matcher.find()) {
            matched = true;
            matchedKeywords.add(matcher.group());
        }
        return matched;
    }
}