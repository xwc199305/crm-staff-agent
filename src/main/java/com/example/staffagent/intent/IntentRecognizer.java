package com.example.staffagent.intent;

import com.example.staffagent.dto.IntentResult;

public interface IntentRecognizer {
    IntentResult recognize(String query);
}