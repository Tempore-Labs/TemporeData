package org.temporedata.modules.gov.security.controller;

import lombok.RequiredArgsConstructor;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.modules.gov.security.entity.QaReportEntity;
import org.temporedata.modules.gov.security.entity.VulnEntity;
import org.temporedata.modules.gov.security.service.SecurityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * P3-13 supply-chain & QA REST API.
 */
@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityService securityService;

    @GetMapping("/vulns")
    public BaseResponse<List<VulnEntity>> vulns() {
        return BaseResponse.success(securityService.vulns());
    }

    @PostMapping("/vulns")
    public BaseResponse<VulnEntity> createVuln(@RequestBody VulnEntity v) {
        return BaseResponse.success(securityService.createVuln(v));
    }

    @PostMapping("/vulns/{id}/status")
    public BaseResponse<VulnEntity> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        return BaseResponse.success(securityService.updateStatus(id, body.get("status")));
    }

    @PostMapping("/vulns/{id}/accept")
    public BaseResponse<VulnEntity> accept(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        return BaseResponse.success(securityService.accept(id,
                body == null ? null : body.get("mitigation")));
    }

    @GetMapping("/sbom")
    public BaseResponse<Map<String, Object>> sbom(@RequestParam(required = false) String version) {
        return BaseResponse.success(securityService.sbom(version));
    }

    @GetMapping("/gate")
    public BaseResponse<Map<String, Object>> gate() {
        return BaseResponse.success(securityService.gateCheck());
    }

    @GetMapping("/qa")
    public BaseResponse<List<QaReportEntity>> qa(@RequestParam(required = false) String version) {
        return BaseResponse.success(securityService.qaReports(version));
    }

    @PostMapping("/qa")
    public BaseResponse<QaReportEntity> addQa(@RequestBody QaReportEntity q) {
        return BaseResponse.success(securityService.addQa(q));
    }
}