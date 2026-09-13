package org.temporedata.modules.gov.security.aipolicy;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.security.aipolicy.AiCommandRequest;
import org.temporedata.security.aipolicy.AiPolicyDecision;
import org.temporedata.security.aipolicy.AiSafetyPolicyEngine;

/**
 * [ENT-P2] AI Safety gate probe: evaluate an AI-generated command through the shared
 * policy engine (RBAC + data classification + risk blocking).
 */
@RestController
@RequestMapping("/api/ai-safety")
public class AiSafetyProbeController {

    private final AiSafetyPolicyEngine engine;

    public AiSafetyProbeController(AiSafetyPolicyEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/check")
    public BaseResponse<AiPolicyDecision> check(@RequestBody AiCommandRequest request,
                                                @RequestParam(required = false) String[] admins) {
        return BaseResponse.success(engine.evaluate(request, admins));
    }
}