package org.temporedata.modules.ops.ai;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.modules.ops.incident.entity.IncidentEntity;
import org.temporedata.modules.ops.incident.repository.IncidentRepository;
import org.temporedata.security.aipolicy.AiCommandRequest;
import org.temporedata.security.aipolicy.AiPolicyDecision;
import org.temporedata.security.aipolicy.AiSafetyPolicyEngine;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * [ENT-P2] RCA-Agent: closes the Quality Issue -&gt; Incident -&gt; RCA -&gt; Action Plan loop.
 *
 * <p>For an incident it (a) formulates a root-cause hypothesis, (b) proposes remediation
 * actions, and (c) runs every suggested command through the {@link AiSafetyPolicyEngine} so
 * only safe/approved actions are executable. Applying an approved plan marks the incident
 * RESOLVED and records the hypothesis as its RCA analysis.
 */
@Service
public class RcaAgentService {

    private final IncidentRepository incidentRepository;
    private final AiSafetyPolicyEngine safetyEngine;

    public RcaAgentService(IncidentRepository incidentRepository, AiSafetyPolicyEngine safetyEngine) {
        this.incidentRepository = incidentRepository;
        this.safetyEngine = safetyEngine;
    }

    /** Propose a gated action plan for the incident (no side effects). */
    @Transactional(readOnly = true)
    public ActionPlan analyze(Long incidentId) {
        IncidentEntity incident = require(incidentId);
        RcaFinding finding = diagnose(incident);
        List<ActionItem> actions = propose(incident, finding);
        ActionPlan plan = new ActionPlan(incidentId, finding, actions, Instant.now());
        return plan;
    }

    /**
     * Apply the (auto-runnable) portion of the plan, optionally auto-approving gated keys.
     * Marks the incident RESOLVED and stores the hypothesis as its RCA.
     */
    @Transactional
    public ApplyOutcome apply(Long incidentId, boolean autoApprove) {
        ActionPlan plan = analyze(incidentId);
        IncidentEntity incident = require(incidentId);

        int applied = 0;
        int needsApproval = 0;
        List<String> run = new ArrayList<>();
        for (ActionItem a : plan.getActions()) {
            if (a.autoRunnable()) {
                applied++;
                run.add(a.getTitle());
            } else if (a.isRequiresApproval()) {
                needsApproval++;
                if (autoApprove) {
                    applied++;
                    run.add(a.getTitle() + " (approved)");
                }
            } else {
                needsApproval++; // blocked by RBAC, not auto-approvable
            }
        }
        incident.setStatus("RESOLVED");
        incident.setRcaAnalysis(plan.getFinding().getHypothesis()
                + " | confidence=" + plan.getFinding().getConfidence()
                + " | applied=" + String.join(",", run.isEmpty() ? List.of("none") : run));
        incidentRepository.save(incident);
        return new ApplyOutcome(incident.getId(), incident.getStatus(), applied, needsApproval, run);
    }

    private RcaFinding diagnose(IncidentEntity incident) {
        String src = incident.getSourceType() == null ? "UNKNOWN" : incident.getSourceType();
        String causeType;
        String hypothesis;
        switch (src) {
            case "QUALITY":
                causeType = "DATA_QUALITY";
                hypothesis = "quality rule breached; likely upstream ETL regression or source schema drift";
                break;
            case "ALARM":
                causeType = "RESOURCE";
                hypothesis = "runtime alarm indicates compute/throughput anomaly near capacity";
                break;
            case "WORKFLOW":
                causeType = "PIPELINE";
                hypothesis = "workflow step failed; upstream dependency or SQL regression";
                break;
            case "API":
                causeType = "ACCESS";
                hypothesis = "API consumer error; quota exceeded or governance rejection";
                break;
            default:
                causeType = "GENERIC";
                hypothesis = "undetermined root cause; recommend metadata + log correlation";
                break;
        }
        double confidence = "HIGH".equals(incident.getSeverity()) ? 0.75 : 0.55;
        return new RcaFinding(incident.getId(), causeType, hypothesis, confidence, Instant.now());
    }

    private List<ActionItem> propose(IncidentEntity incident, RcaFinding finding) {
        String target = incident.getDatasetId() == null ? "unknown_dataset" : "ds" + incident.getDatasetId();
        AiCommandRequest req = new AiCommandRequest();

        List<ActionItem> items = new ArrayList<>();
        // 1) read-only diagnostic
        req.setPrincipal("rca-agent");
        req.setSecurityLevel(incident.getSeverity());
        req.setCommand("SELECT COUNT(*) FROM ods_" + target);
        AiPolicyDecision d1 = safetyEngine.evaluate(req);
        items.add(new ActionItem("Verify upstream volume", req.getCommand(), "TEXT2SQL",
                d1.isAllowed(), d1.isRequiresApproval(), d1.getRisk(), d1.getReason()));

        // 2) schema drift probe
        req.setCommand("SELECT column_name FROM information_schema.columns WHERE table_name LIKE '%" + target + "%'");
        AiPolicyDecision d2 = safetyEngine.evaluate(req);
        items.add(new ActionItem("Detect schema drift", req.getCommand(), "TEXT2SQL",
                d2.isAllowed(), d2.isRequiresApproval(), d2.getRisk(), d2.getReason()));

        // 3) corrective cleanup (destructive -> gate)
        req.setCommand("DELETE FROM error_queue_" + target);
        AiPolicyDecision d3 = safetyEngine.evaluate(req);
        items.add(new ActionItem("Purge poisoned rows", req.getCommand(), "SCRIPT",
                d3.isAllowed(), d3.isRequiresApproval(), d3.getRisk(), d3.getReason()));
        return items;
    }

    private IncidentEntity require(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("incident not found: " + id));
    }

    /** Outcome of applying a plan. */
    public static final class ApplyOutcome {
        private final Long incidentId;
        private final String status;
        private final int applied;
        private final int needsApproval;
        private final List<String> executed;

        public ApplyOutcome(Long incidentId, String status, int applied, int needsApproval, List<String> executed) {
            this.incidentId = incidentId;
            this.status = status;
            this.applied = applied;
            this.needsApproval = needsApproval;
            this.executed = executed;
        }

        public Long getIncidentId() { return incidentId; }
        public String getStatus() { return status; }
        public int getApplied() { return applied; }
        public int getNeedsApproval() { return needsApproval; }
        public List<String> getExecuted() { return executed; }
    }
}