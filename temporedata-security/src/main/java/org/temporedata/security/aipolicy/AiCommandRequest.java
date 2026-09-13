package org.temporedata.security.aipolicy;

/**
 * [ENT-P2] A command submitted by AI (Text2SQL / DDL / Action Plan script) for safety
 * evaluation. Carries enough context to reproduce the same RBAC &amp; data-classification
 * checks a human operator would face.
 */
public final class AiCommandRequest {

    private AiActionType type;
    private String principal;
    private Long targetDatasetId;
    private String target;
    private String securityLevel;
    private String command;
    private boolean autoApproved;

    public AiActionType getType() { return type; }
    public void setType(AiActionType type) { this.type = type; }

    public String getPrincipal() { return principal; }
    public void setPrincipal(String principal) { this.principal = principal; }

    public Long getTargetDatasetId() { return targetDatasetId; }
    public void setTargetDatasetId(Long targetDatasetId) { this.targetDatasetId = targetDatasetId; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getSecurityLevel() { return securityLevel; }
    public void setSecurityLevel(String securityLevel) { this.securityLevel = securityLevel; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public boolean isAutoApproved() { return autoApproved; }
    public void setAutoApproved(boolean autoApproved) { this.autoApproved = autoApproved; }
}