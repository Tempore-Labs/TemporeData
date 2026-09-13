package org.temporedata.gateway.service;

import org.springframework.stereotype.Service;

/**
 * [ENT-P1] AppKey verification for the Data API gateway.
 *
 * <p>Skeleton: validates an AppKey against an expected key (constant-time compare to resist
 * timing attacks). In production the expected key comes from an app registry / secret store;
 * when unconfigured it only requires a non-blank key so the demo flow stays usable.
 */
@Service
public class ApiKeyVerifier {

    /** Configured expected key; empty disables strict matching. */
    private String expectedKey;

    public ApiKeyVerifier(String expectedKey) {
        this.expectedKey = expectedKey == null ? "" : expectedKey;
    }

    public ApiKeyVerifier() {
        this("");
    }

    public void setExpectedKey(String expectedKey) {
        this.expectedKey = expectedKey == null ? "" : expectedKey;
    }

    public boolean verify(String provided) {
        if (provided == null || provided.isBlank()) {
            return false;
        }
        if (expectedKey.isEmpty()) {
            return true; // unconfigured: any non-blank AppKey accepted (demo bootstrap)
        }
        return constantTimeEquals(expectedKey, provided);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}