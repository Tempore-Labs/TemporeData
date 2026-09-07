package org.temporedata.modules.dev.perm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.modules.dev.perm.entity.PermissionEntity;
import org.temporedata.modules.dev.perm.entity.ResourceEntity;
import org.temporedata.modules.dev.perm.repository.PermissionRepository;
import org.temporedata.modules.dev.perm.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * P3-11 resource-level RBAC: resource catalog, role-resource permission matrix,
 * and the authorization engine (DENY overrides ALLOW, wildcard '*' supported).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // action seniority: higher rank covers lower ones (ADMIN > EXECUTE > WRITE > READ)
    private static final Map<String, Integer> RANK = new LinkedHashMap<>();
    static {
        RANK.put("READ", 1);
        RANK.put("WRITE", 2);
        RANK.put("EXECUTE", 3);
        RANK.put("ADMIN", 4);
    }

    private final ResourceRepository resourceRepository;
    private final PermissionRepository permissionRepository;

    // ---- Resource catalog ----

    @Transactional(readOnly = true)
    public List<ResourceEntity> resources(String resourceType, String name) {
        if (resourceType != null && !resourceType.isBlank()) {
            return resourceRepository.findByResourceType(resourceType);
        }
        List<ResourceEntity> all = resourceRepository.findAll();
        if (name == null || name.isBlank()) return all;
        return all.stream().filter(r -> name.equals(r.getResourceName())).collect(Collectors.toList());
    }

    @Transactional
    public ResourceEntity registerResource(String resourceType, String resourceKey, String resourceName,
                                           String owner, String tenantId) {
        ResourceEntity res = resourceRepository.findByResourceTypeAndResourceKey(resourceType, resourceKey)
                .orElseGet(() -> ResourceEntity.builder()
                        .resourceType(resourceType).resourceKey(resourceKey).createTime(now()).build());
        res.setResourceName(resourceName);
        res.setOwner(owner);
        res.setTenantId(tenantId);
        return resourceRepository.save(res);
    }

    @Transactional
    public void deleteResource(String id) {
        resourceRepository.deleteById(id);
    }

    // ---- Permission matrix ----

    @Transactional
    public PermissionEntity grant(PermissionEntity p) {
        p.setId(null);
        p.setScope(p.getScope() == null ? "ALLOW" : p.getScope());
        if (p.getResourceKey() == null || p.getResourceKey().isBlank()) p.setResourceKey("*");
        p.setCreateTime(now());
        return permissionRepository.save(p);
    }

    @Transactional
    public void revoke(String id) {
        permissionRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<PermissionEntity> permissions(List<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return permissionRepository.findAll();
        return permissionRepository.findByRoleIdIn(roleIds);
    }

    // ---- Authorization engine ----

    /**
     * Verify a role set can perform <code>action</code> on a resource.
     * A super admin bypasses. DENY entries override ALLOW. A granted action also
     * implies its lower-ranked actions.
     */
    @Transactional(readOnly = true)
    public boolean verify(List<String> roleIds, boolean superAdmin,
                          String resourceType, String action, String resourceKey) {
        if (superAdmin) return true;
        if (roleIds == null || roleIds.isEmpty()) return false;
        int needed = RANK.getOrDefault(action == null ? "READ" : action.toUpperCase(), 1);
        List<PermissionEntity> perms = permissionRepository
                .findByResourceTypeAndRoleIdIn(resourceType, roleIds);

        // DENY first
        for (PermissionEntity p : perms) {
            if (!"DENY".equalsIgnoreCase(p.getScope())) continue;
            if (keyMatch(p.getResourceKey(), resourceKey) && rank(p.getAction()) >= needed) return false;
        }
        // ALLOW
        for (PermissionEntity p : perms) {
            if (!"ALLOW".equalsIgnoreCase(p.getScope())) continue;
            if (keyMatch(p.getResourceKey(), resourceKey) && rank(p.getAction()) >= needed) return true;
        }
        return false;
    }

    private boolean keyMatch(String grantedKey, String targetKey) {
        if ("*".equals(grantedKey)) return true;
        return grantedKey != null && grantedKey.equals(targetKey);
    }

    /**
     * Expand the permission matrix of a role set into permission points for the
     * frontend (menu/button visibility).
     */
    @Transactional(readOnly = true)
    public List<String> myPerms(List<String> roleIds) {
        List<String> points = new ArrayList<>();
        if (roleIds == null || roleIds.isEmpty()) return points;
        for (PermissionEntity p : permissionRepository.findByRoleIdIn(roleIds)) {
            if ("DENY".equalsIgnoreCase(p.getScope())) continue;
            points.add("permission:" + p.getResourceType() + ":" + p.getAction().toLowerCase());
            points.add("resource:" + p.getResourceType() + ":" + p.getResourceKey() + ":" + p.getAction().toLowerCase());
        }
        return points;
    }

    /**
     * Filter resources of a type by what the role set can at least read.
     */
    @Transactional(readOnly = true)
    public List<ResourceEntity> accessibleResources(List<String> roleIds, boolean superAdmin, String resourceType) {
        List<ResourceEntity> all = resourceType == null || resourceType.isBlank()
                ? resourceRepository.findAll() : resourceRepository.findByResourceType(resourceType);
        if (superAdmin) return all;
        if (roleIds == null || roleIds.isEmpty()) return new ArrayList<>();
        List<ResourceEntity> out = new ArrayList<>();
        for (ResourceEntity r : all) {
            if (verify(roleIds, false, r.getResourceType(), "READ", r.getResourceKey())) out.add(r);
        }
        return out;
    }

    private int rank(String action) {
        Integer r = RANK.get(action == null ? null : action.toUpperCase());
        return r == null ? 1 : r;
    }

    private String now() {
        return LocalDateTime.now().format(DTF);
    }
}