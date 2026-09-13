package org.temporedata.modules.ops.ai;

import org.temporedata.security.aipolicy.AiRisk;

/**
 * One recommended action in an RCA {@link ActionPlan}. Already evaluated by the AI safety
 * gate, so callers know whether it may run stand-alone, needs approval, or is blocked.
 */
public final class ActionItem {

    private final String title;
    private final String suggestedCommand;
    private final String actionType;
    private final boolean allowed;
    private final boolean requiresApproval;
    private final AiRisk risk;
    private final String reason;

    public ActionItem(String title, String suggestedCommand, String actionType,
                      boolean allowed, boolean requiresApproval, AiRisk risk, String reason) {
        this.title = title;
        this.suggestedCommand = suggestedCommand;
        this.actionType = actionType;
        this.allowed = allowed;
        this.requiresApproval = requiresApproval;
        this.risk = risk;
        this.reason = reason;
    }

    public String getTitle() { return title; }
    public String getSuggestedCommand() { return suggestedCommand; }
    public String getActionType() { return actionType; }
    public boolean isAllowed() { return allowed; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public AiRisk getRisk() { return risk; }
    public String getReason() { return reason; }

    /** Runnable without further sign-off. */
    public boolean autoRunnable() {
        return allowed && !requiresApproval;
    }
}