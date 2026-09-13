package org.temporedata.modules.ops.ai;

import java.time.Instant;
import java.util.List;

/**
 * RCA-Agent output: root-cause hypothesis + gated action plan for one incident.
 */
public final class ActionPlan {

    private final Long incidentId;
    private final RcaFinding finding;
    private final List<ActionItem> actions;
    private final Instant at;

    public ActionPlan(Long incidentId, RcaFinding finding, List<ActionItem> actions, Instant at) {
        this.incidentId = incidentId;
        this.finding = finding;
        this.actions = actions;
        this.at = at;
    }

    public Long getIncidentId() { return incidentId; }
    public RcaFinding getFinding() { return finding; }
    public List<ActionItem> getActions() { return actions; }
    public Instant getAt() { return at; }

    public long autoRunnableCount() {
        return actions.stream().filter(ActionItem::autoRunnable).count();
    }
}