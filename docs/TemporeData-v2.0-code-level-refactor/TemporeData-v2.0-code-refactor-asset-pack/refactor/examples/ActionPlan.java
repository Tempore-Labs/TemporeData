package org.temporedata.api.contract.automation;

import java.util.List;
import java.util.Map;

public record ActionPlan(
    String id,
    String incidentId,
    String actionType,
    String targetId,
    Map<String, Object> parameters,
    RiskLevel risk,
    String reason,
    List<Evidence> evidence
) {}
