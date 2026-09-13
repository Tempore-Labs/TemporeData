package org.temporedata.modules.ops.ai.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.modules.ops.ai.ActionPlan;
import org.temporedata.modules.ops.ai.RcaAgentService;

/**
 * [ENT-P2] RCA-Agent REST endpoints: propose an action plan and/or apply the safe portion.
 */
@RestController
@RequestMapping("/api/rca")
public class RcaAgentController {

    private final RcaAgentService rcaAgentService;

    public RcaAgentController(RcaAgentService rcaAgentService) {
        this.rcaAgentService = rcaAgentService;
    }

    /** Propose a gated action plan for an incident (no side effects). */
    @PostMapping("/{incidentId}/analyze")
    public BaseResponse<ActionPlan> analyze(@PathVariable Long incidentId) {
        return BaseResponse.success(rcaAgentService.analyze(incidentId));
    }

    /** Apply auto-runnable actions (optionally approving gated keys) and mark the incident resolved. */
    @PostMapping("/{incidentId}/apply")
    public BaseResponse<RcaAgentService.ApplyOutcome> apply(@PathVariable Long incidentId,
                                                            @RequestParam(defaultValue = "false") boolean approve) {
        return BaseResponse.success(rcaAgentService.apply(incidentId, approve));
    }
}