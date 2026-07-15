package com.example.staffagent.context;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConversationContextHolder {

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    private ConversationContextHolder() {
    }

    public static void setContext(String context) {
        CONTEXT_HOLDER.set(context);
        log.debug("Set conversation context, length={}", context != null ? context.length() : 0);
    }

    public static String getContext() {
        return CONTEXT_HOLDER.get();
    }

    public static void clearContext() {
        CONTEXT_HOLDER.remove();
        log.debug("Cleared conversation context");
    }

    public static boolean hasContext() {
        return CONTEXT_HOLDER.get() != null && !CONTEXT_HOLDER.get().isEmpty();
    }
}