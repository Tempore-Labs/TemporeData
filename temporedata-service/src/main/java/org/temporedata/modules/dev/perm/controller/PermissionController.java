package org.temporedata.modules.dev.perm.controller;

import lombok.RequiredArgsConstructor;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.modules.dev.perm.entity.PermissionEntity;
import org.temporedata.modules.dev.perm.entity.ResourceEntity;
import org.temporedata.modules.dev.perm.service.PermissionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * P3-11 permission center REST API.
 */
@RestController
@RequestMapping("/api/perm")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    private List<String> roleIds(String roleIds) {
        if (roleIds == null || roleIds.isBlank()) return List.of();
        return Arrays.asList(roleIds.split(","));
    }

    private String currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "anonymous" : auth.getName();
    }

    @GetMapping("/resources")
    public BaseResponse<List<ResourceEntity>> resources(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String name) {
        return BaseResponse.success(permissionService.resources(resourceType, name));
    }

    @PostMapping("/resources")
    public BaseResponse<ResourceEntity> register(@RequestBody ResourceEntity r) {
        return BaseResponse.success(permissionService.registerResource(
                r.getResourceType(), r.getResourceKey(), r.getResourceName(),
                r.getOwner() == null ? currentUser() : r.getOwner(), r.getTenantId()));
    }

    @DeleteMapping("/resources/{id}")
    public BaseResponse<Void> deleteResource(@PathVariable String id) {
        permissionService.deleteResource(id);
        return BaseResponse.success();
    }

    @GetMapping("/permissions")
    public BaseResponse<List<PermissionEntity>> permissions(
            @RequestParam(required = false) String roleIds) {
        return BaseResponse.success(permissionService.permissions(roleIds(roleIds)));
    }

    @PostMapping("/permissions")
    public BaseResponse<PermissionEntity> grant(@RequestBody PermissionEntity p) {
        return BaseResponse.success(permissionService.grant(p));
    }

    @DeleteMapping("/permissions/{id}")
    public BaseResponse<Void> revoke(@PathVariable String id) {
        permissionService.revoke(id);
        return BaseResponse.success();
    }

    @GetMapping("/my/perms")
    public BaseResponse<List<String>> myPerms(@RequestParam(required = false) String roleIds) {
        return BaseResponse.success(permissionService.myPerms(roleIds(roleIds)));
    }

    @PostMapping("/verify")
    public BaseResponse<Boolean> verify(@RequestBody Map<String, String> body) {
        return BaseResponse.success(permissionService.verify(
                roleIds(body.get("roleIds")),
                Boolean.parseBoolean(body.getOrDefault("superAdmin", "false")),
                body.get("resourceType"), body.get("action"), body.get("resourceKey")));
    }

    @GetMapping("/resources/{type}/accessible")
    public BaseResponse<List<ResourceEntity>> accessible(
            @PathVariable String type,
            @RequestParam(required = false, defaultValue = "false") boolean superAdmin,
            @RequestParam(required = false) String roleIds) {
        return BaseResponse.success(permissionService.accessibleResources(roleIds(roleIds), superAdmin, type));
    }
}