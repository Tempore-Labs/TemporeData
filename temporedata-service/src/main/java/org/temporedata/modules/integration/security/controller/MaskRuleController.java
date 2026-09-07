package org.temporedata.modules.integration.security.controller;

import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.modules.integration.security.entity.MaskRuleEntity;
import org.temporedata.modules.integration.security.service.MaskRuleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MaskRule CRUD (脱敏规则管理, P1). Secured under /api/**.
 */
@RestController
@RequestMapping("/api/security/mask-rule")
public class MaskRuleController {

    private final MaskRuleService service;

    public MaskRuleController(MaskRuleService service) {
        this.service = service;
    }

    @GetMapping
    public BaseResponse<List<MaskRuleEntity>> list() {
        return BaseResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public BaseResponse<MaskRuleEntity> get(@PathVariable String id) {
        return BaseResponse.success(service.get(id));
    }

    @PostMapping
    public BaseResponse<MaskRuleEntity> create(@RequestBody MaskRuleEntity entity) {
        return BaseResponse.success(service.create(entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<MaskRuleEntity> update(@PathVariable String id, @RequestBody MaskRuleEntity entity) {
        return BaseResponse.success(service.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return BaseResponse.success();
    }
}