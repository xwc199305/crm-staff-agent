package com.example.staffagent.mcp;

/** Holds Salesforce credentials only for the lifetime of one synchronous agent request. */
public final class SalesforceMcpRequestContext {

    private static final ThreadLocal<Credentials> CREDENTIALS = new ThreadLocal<>();

    private SalesforceMcpRequestContext() {
    }

    public static void set(String orgDomain, String authorization) {
        String accessToken = extractBearerToken(authorization);
        if (hasText(orgDomain) && hasText(accessToken)) {
            CREDENTIALS.set(new Credentials(orgDomain, accessToken));
        } else {
            CREDENTIALS.remove();
        }
    }

    public static Credentials get() {
        return CREDENTIALS.get();
    }

    public static void clear() {
        CREDENTIALS.remove();
    }

    private static String extractBearerToken(String authorization) {
        if (!hasText(authorization)) {
            return null;
        }
        String prefix = "Bearer ";
        return authorization.regionMatches(true, 0, prefix, 0, prefix.length())
                ? authorization.substring(prefix.length()).trim()
                : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record Credentials(String orgDomain, String accessToken) {
    }
}
