package org.temporedata.modules.sys.role.controller;

import org.temporedata.modules.sys.role.entity.RoleEntity;
import org.temporedata.modules.sys.role.service.RoleService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public BaseResponse<List<RoleEntity>> list() {
        return BaseResponse.success(roleService.list());
    }

    @PostMapping
    public BaseResponse<RoleEntity> create(@RequestBody RoleEntity entity) {
        return BaseResponse.success(roleService.create(entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<RoleEntity> update(@PathVariable String id, @RequestBody RoleEntity entity) {
        return BaseResponse.success(roleService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        roleService.delete(id);
        return BaseResponse.success();
    }

    @GetMapping("/{id}/members")
    public BaseResponse<List<Map<String, Object>>> getMembers(@PathVariable String id) {
        return BaseResponse.success(roleService.getMembers(id));
    }
}