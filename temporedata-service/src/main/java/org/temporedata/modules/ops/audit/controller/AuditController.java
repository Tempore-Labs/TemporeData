package org.temporedata.modules.ops.audit.controller;

import lombok.RequiredArgsConstructor;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.modules.ops.audit.entity.AuditArchiveEntity;
import org.temporedata.modules.ops.audit.entity.AuditEventEntity;
import org.temporedata.modules.ops.audit.entity.AuditPolicyEntity;
import org.temporedata.modules.ops.audit.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * P3-12 unified audit REST API (hash-chain verified).
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/events")
    public BaseResponse<List<AuditEventEntity>> events(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operator) {
        return BaseResponse.success(auditService.list(module, operator));
    }

    @GetMapping("/events/page")
    public BaseResponse<Page<AuditEventEntity>> eventsPage(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            Pageable pageable) {
        return BaseResponse.success(auditService.page(eventType, operator, action, resourceType, pageable));
    }

    @GetMapping("/events/{id}")
    public BaseResponse<Map<String, Object>> get(@PathVariable String id) {
        return BaseResponse.success(auditService.get(id));
    }

    @PostMapping("/events")
    public BaseResponse<AuditEventEntity> record(@RequestBody Map<String, String> body) {
        return BaseResponse.success(auditService.record(
                body.get("module"), body.get("action"),
                body.get("resourceType"), body.get("resourceKey"),
                body.get("operator"), body.get("ip"), body.get("reqId"),
                body.get("status"), body.get("detailJson")));
    }

    @PostMapping("/events/submit")
    public BaseResponse<AuditEventEntity> submit(@RequestBody Map<String, String> body) {
        return BaseResponse.success(auditService.submit(
                body.get("eventType"), body.get("action"),
                body.get("resourceType"), body.get("resourceKey"),
                body.get("operator"), body.get("ip"),
                body.get("status"), body.get("detailJson")));
    }

    @PostMapping("/verify/range")
    public BaseResponse<Map<String, Object>> verifyRange(@RequestBody(required = false) Map<String, String> body) {
        return BaseResponse.success(auditService.verifyRange(
                body == null ? null : body.get("from"), body == null ? null : body.get("to")));
    }

    @GetMapping("/export")
    public BaseResponse<Map<String, Object>> export(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return BaseResponse.success(auditService.export(from, to));
    }

    @PostMapping("/archive")
    public BaseResponse<AuditArchiveEntity> archive() {
        return BaseResponse.success(auditService.archive());
    }

    @GetMapping("/archives")
    public BaseResponse<List<AuditArchiveEntity>> archives() {
        return BaseResponse.success(auditService.archives());
    }

    @GetMapping("/policies")
    public BaseResponse<List<AuditPolicyEntity>> policies() {
        return BaseResponse.success(auditService.policies());
    }

    @PostMapping("/policies")
    public BaseResponse<AuditPolicyEntity> savePolicy(@RequestBody AuditPolicyEntity p) {
        return BaseResponse.success(auditService.savePolicy(p));
    }
}