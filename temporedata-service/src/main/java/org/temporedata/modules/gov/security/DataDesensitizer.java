package org.temporedata.modules.gov.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Desensitization function registry (P0, 数据访问控制与动态脱敏执行链 v1.0 §5).
 * Pure, O(n), precomputed patterns; designed to be cheap so it can run on a
 * streamed result without becoming a bottleneck on large datasets.
 */
@Component
public class DataDesensitizer {

    /** Apply a masking rule to a single value. Returns null for DROP/null input. */
    public String apply(String ruleType, String maskPattern, String value) {
        if (ruleType == null || value == null) {
            return value;
        }
        switch (ruleType.trim().toUpperCase()) {
            case "PHONE": return phone(value);
            case "NAME": return name(value);
            case "EMAIL": return email(value);
            case "ID_CARD": return idCard(value);
            case "BANK_CARD": return bankCard(value);
            case "CUSTOM": return custom(value, maskPattern);
            case "HASH": return hash(value);
            case "DROP": return null;
            default: return "***";
        }
    }

    private String phone(String v) {
        return v.length() >= 7
                ? v.substring(0, 3) + "****" + v.substring(v.length() - 4)
                : "****";
    }

    private String name(String v) {
        return v.length() >= 2 ? v.charAt(0) + "**" : "**";
    }

    private String email(String v) {
        int at = v.indexOf('@');
        return at > 0 ? v.charAt(0) + "***" + v.substring(at) : "***";
    }

    private String idCard(String v) {
        return v.length() >= 10
                ? v.substring(0, 6) + "********" + v.substring(v.length() - 4)
                : (v.length() >= 2 ? v.charAt(0) + "*" : "*");
    }

    private String bankCard(String v) {
        return v.length() >= 8
                ? v.substring(0, 4) + " **** **** " + v.substring(v.length() - 4)
                : v.replaceAll(".", "*");
    }

    private String custom(String v, String pattern) {
        String p = (pattern == null || pattern.isEmpty()) ? "****" : pattern;
        return v.length() > 8
                ? v.substring(0, 4) + p + v.substring(v.length() - 4)
                : p;
    }

    private String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, Math.min(12, sb.length()));
        } catch (Exception e) {
            return String.valueOf(value.hashCode());
        }
    }
}