package org.temporedata.modules.asset.permapproval.controller;

import org.temporedata.modules.dev.approval.engine.ApprovalEngineService;
import org.temporedata.modules.asset.permapproval.entity.PermapprovalEntity;
import org.temporedata.modules.asset.permapproval.service.PermapprovalService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permapproval")
@RequiredArgsConstructor
public class PermapprovalController {

    private static final String TYPE = "RESOURCE_PERM";

    private final PermapprovalService permapprovalService;
    private final ApprovalEngineService approvalEngine;

    @GetMapping("/page")
    public BaseResponse<org.springframework.data.domain.Page<PermapprovalEntity>> page(
            org.springframework.data.domain.Pageable pageable) {
        return BaseResponse.success(permapprovalService.page(pageable));
    }

    @GetMapping("/list")
    public BaseResponse<List<PermapprovalEntity>> list() {
        return BaseResponse.success(permapprovalService.list());
    }

    @GetMapping("/pending")
    public BaseResponse<List<PermapprovalEntity>> pending() {
        return BaseResponse.success(approvalEngine.listPending(TYPE));
    }

    @GetMapping("/my")
    public BaseResponse<List<PermapprovalEntity>> my(@RequestParam(required = false) String applicantId) {
        return BaseResponse.success(approvalEngine.listByApplicant(TYPE, applicantId));
    }

    @GetMapping("/{id}")
    public BaseResponse<PermapprovalEntity> get(@PathVariable String id) {
        return BaseResponse.success(approvalEngine.get(TYPE, id));
    }

    @PostMapping("/create")
    public BaseResponse<PermapprovalEntity> create(@RequestBody PermapprovalEntity entity) {
        return BaseResponse.success(approvalEngine.apply(TYPE, entity));
    }

    @PostMapping
    public BaseResponse<PermapprovalEntity> createCompat(@RequestBody PermapprovalEntity entity) {
        return BaseResponse.success(approvalEngine.apply(TYPE, entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<PermapprovalEntity> update(@PathVariable String id, @RequestBody PermapprovalEntity entity) {
        entity.setId(id);
        return BaseResponse.success(permapprovalService.update(entity));
    }

    @PostMapping("/{id}/approve")
    public BaseResponse<PermapprovalEntity> approve(@PathVariable String id) {
        return BaseResponse.success(approvalEngine.approve(TYPE, id));
    }

    @PostMapping("/{id}/reject")
    public BaseResponse<PermapprovalEntity> reject(@PathVariable String id) {
        return BaseResponse.success(approvalEngine.reject(TYPE, id));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        approvalEngine.delete(TYPE, id);
        return BaseResponse.success();
    }
}