package org.temporedata.modules.ops.ai;

import java.time.Instant;

/**
 * RCA root-cause hypothesis produced by {@link RcaAgentService}.
 */
public final class RcaFinding {

    private final Long incidentId;
    private final String causeType;
    private final String hypothesis;
    private final double confidence;
    private final Instant at;

    public RcaFinding(Long incidentId, String causeType, String hypothesis, double confidence, Instant at) {
        this.incidentId = incidentId;
        this.causeType = causeType;
        this.hypothesis = hypothesis;
        this.confidence = confidence;
        this.at = at;
    }

    public Long getIncidentId() { return incidentId; }
    public String getCauseType() { return causeType; }
    public String getHypothesis() { return hypothesis; }
    public double getConfidence() { return confidence; }
    public Instant getAt() { return at; }
}