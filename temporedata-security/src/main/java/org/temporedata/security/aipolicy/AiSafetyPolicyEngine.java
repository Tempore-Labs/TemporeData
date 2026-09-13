package org.temporedata.security.aipolicy;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * [ENT-P2] AI Safety Policy engine.
 *
 * <p>Evaluate AI-generated SQL / DDL / Action-Plan scripts under the same RBAC, data
 * classification and risk-blocking rules that a human operator obeys:
 * destructive commands (drop / truncate / delete / alter) on sensitive targets require an
 * elevated principal and, for classified datasets, an explicit approval.
 */
@Service("aiSafetyPolicyEngine")
public class AiSafetyPolicyEngine {

    private final Set<String> adminPrincipals;

    public AiSafetyPolicyEngine() {
        this.adminPrincipals = Set.of("admin");
    }

    /** Keys of high-security dataset classifications that always require approval for writes. */
    private static final Set<String> SENSITIVE_LEVELS = Set.of("S3", "S4", "HIGH");

    /** Keywords that classify a command as destructive (mutating / dropping). */
    private static final String[] DESTRUCTIVE_KEYWORDS = {"DROP", "TRUNCATE", "DELETE", "ALTER"};
    /** Base risk per destructive keyword. */
    private static final String[] CRITICAL_KEYWORDS = {"DROP", "TRUNCATE"};

    /**
     * Evaluate a command and return the policy decision.
     *
     * @param configuredAdmins optional override of elevated principals (defaults to admin)
     */
    public AiPolicyDecision evaluate(AiCommandRequest req, String... configuredAdmins) {
        Set<String> admins = configuredAdmins == null || configuredAdmins.length == 0
                ? adminPrincipals
                : Arrays.stream(configuredAdmins).collect(Collectors.toSet());

        if (req.getPrincipal() == null || req.getPrincipal().isBlank()) {
            return AiPolicyDecision.deny(AiRisk.LOW, "principal missing (unauthenticated AI action)");
        }

        String cmd = req.getCommand() == null ? "" : req.getCommand().toUpperCase(Locale.ROOT);
        boolean destructive = hasAny(cmd, DESTRUCTIVE_KEYWORDS);
        boolean critical = hasAny(cmd, CRITICAL_KEYWORDS);
        AiRisk risk;
        String kind;
        if (critical) {
            risk = AiRisk.CRITICAL;
            kind = "critically destructive (drop/truncate)";
        } else if (destructive) {
            risk = AiRisk.HIGH;
            kind = "destructive (delete/alter)";
        } else if (hasAny(cmd, "UPDATE", "INSERT")) {
            risk = AiRisk.MEDIUM;
            kind = "data mutation";
        } else {
            risk = AiRisk.LOW;
            kind = "read/plain";
        }

        boolean sensitive = req.getSecurityLevel() != null
                && SENSITIVE_LEVELS.contains(req.getSecurityLevel().toUpperCase(Locale.ROOT));

        if (destructive) {
            if (!admins.contains(req.getPrincipal())) {
                return AiPolicyDecision.deny(risk,
                        "principal '" + req.getPrincipal() + "' lacks elevated RBAC for " + kind);
            }
            if (sensitive && !req.isAutoApproved()) {
                return AiPolicyDecision.approval(risk,
                        "sensitive dataset requires explicit approval for " + kind);
            }
            return AiPolicyDecision.allow(risk,
                    "elevated principal, " + (sensitive ? "approved, " : "") + "risk=" + risk);
        }

        // Non-destructive: principal present is enough (RBAC layer already guards the call).
        return AiPolicyDecision.allow(risk, "non-destructive action, risk=" + risk);
    }

    private boolean hasAny(String upper, String... keywords) {
        if (upper.isEmpty()) {
            return false;
        }
        for (String k : keywords) {
            if (upper.contains(k)) {
                return true;
            }
        }
        return false;
    }
}