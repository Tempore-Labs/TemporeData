package org.temporedata.modules.dev.approval.controller;

import org.temporedata.modules.dev.approval.engine.ApprovalEngineService;
import org.temporedata.modules.dev.approval.entity.ApprovalEntity;
import org.temporedata.modules.dev.approval.service.ApprovalService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private static final String TYPE = "GENERIC";

    private final ApprovalService approvalService;
    private final ApprovalEngineService approvalEngine;

    @GetMapping("/page")
    public BaseResponse<org.springframework.data.domain.Page<ApprovalEntity>> page(
            org.springframework.data.domain.Pageable pageable) {
        return BaseResponse.success(approvalService.page(pageable));
    }

    @GetMapping("/list")
    public BaseResponse<List<ApprovalEntity>> list() {
        return BaseResponse.success(approvalService.list());
    }

    @GetMapping("/pending")
    public BaseResponse<List<ApprovalEntity>> pending() {
        return BaseResponse.success(approvalEngine.listPending(TYPE));
    }

    @GetMapping("/my")
    public BaseResponse<List<ApprovalEntity>> my(@RequestParam(required = false) String applicantId) {
        return BaseResponse.success(approvalEngine.listByApplicant(TYPE, applicantId));
    }

    @GetMapping("/{id}")
    public BaseResponse<ApprovalEntity> get(@PathVariable String id) {
        return BaseResponse.success(approvalEngine.get(TYPE, id));
    }

    @PostMapping("/create")
    public BaseResponse<ApprovalEntity> create(@RequestBody ApprovalEntity entity) {
        return BaseResponse.success(approvalEngine.apply(TYPE, entity));
    }

    @PostMapping
    public BaseResponse<ApprovalEntity> createCompat(@RequestBody ApprovalEntity entity) {
        return BaseResponse.success(approvalEngine.apply(TYPE, entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<ApprovalEntity> update(@PathVariable String id, @RequestBody ApprovalEntity entity) {
        entity.setId(id);
        return BaseResponse.success(approvalService.update(entity));
    }

    @PostMapping("/{id}/approve")
    public BaseResponse<ApprovalEntity> approve(@PathVariable String id) {
        return BaseResponse.success(approvalEngine.approve(TYPE, id));
    }

    @PostMapping("/{id}/reject")
    public BaseResponse<ApprovalEntity> reject(@PathVariable String id) {
        return BaseResponse.success(approvalEngine.reject(TYPE, id));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        approvalEngine.delete(TYPE, id);
        return BaseResponse.success();
    }
}