package org.temporedata.modules.sys.tenant.controller;

import org.temporedata.modules.sys.tenant.entity.TenantEntity;
import org.temporedata.modules.sys.tenant.service.TenantService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    public BaseResponse<List<TenantEntity>> list() {
        return BaseResponse.success(tenantService.list());
    }

    @PostMapping
    public BaseResponse<TenantEntity> create(@RequestBody TenantEntity entity) {
        return BaseResponse.success(tenantService.create(entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<TenantEntity> update(@PathVariable String id, @RequestBody TenantEntity entity) {
        return BaseResponse.success(tenantService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        tenantService.delete(id);
        return BaseResponse.success();
    }

    @GetMapping("/{tenantId}/members")
    public BaseResponse<List<Map<String, Object>>> getMembers(@PathVariable String tenantId) {
        return BaseResponse.success(tenantService.getMembers(tenantId));
    }

    @PostMapping("/{tenantId}/members")
    public BaseResponse<Map<String, Object>> addMember(@PathVariable String tenantId,
                                                       @RequestBody Map<String, Object> member) {
        return BaseResponse.success(tenantService.addMember(tenantId, member));
    }

    @DeleteMapping("/{tenantId}/members/{userId}")
    public BaseResponse<Void> removeMember(@PathVariable String tenantId, @PathVariable String userId) {
        tenantService.removeMember(tenantId, userId);
        return BaseResponse.success();
    }

    @PutMapping("/{tenantId}/members/{userId}/status")
    public BaseResponse<Map<String, Object>> setMemberStatus(@PathVariable String tenantId,
                                                             @PathVariable String userId,
                                                             @RequestBody Map<String, Object> status) {
        return BaseResponse.success(tenantService.setMemberStatus(tenantId, userId, status));
    }
}