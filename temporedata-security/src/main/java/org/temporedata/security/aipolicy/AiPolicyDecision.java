package org.temporedata.security.aipolicy;

import java.util.List;

/**
 * [ENT-P2] Outcome of running one AI command through the safety policy engine.
 */
public final class AiPolicyDecision {

    private final boolean allowed;
    private final AiRisk risk;
    private final String reason;
    private final boolean requiresApproval;

    public AiPolicyDecision(boolean allowed, AiRisk risk, String reason, boolean requiresApproval) {
        this.allowed = allowed;
        this.risk = risk;
        this.reason = reason;
        this.requiresApproval = requiresApproval;
    }

    public static AiPolicyDecision allow(AiRisk risk, String reason) {
        return new AiPolicyDecision(true, risk, reason, false);
    }

    public static AiPolicyDecision deny(AiRisk risk, String reason) {
        return new AiPolicyDecision(false, risk, reason, false);
    }

    public static AiPolicyDecision approval(AiRisk risk, String reason) {
        return new AiPolicyDecision(false, risk, reason, true);
    }

    public boolean isAllowed() { return allowed; }
    public AiRisk getRisk() { return risk; }
    public String getReason() { return reason; }
    public boolean isRequiresApproval() { return requiresApproval; }
}