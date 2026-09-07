package org.temporedata.modules.gov.lineage.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.temporedata.modules.dev.perm.service.PermissionService;
import org.temporedata.modules.sys.role.entity.RoleEntity;
import org.temporedata.modules.sys.role.entity.UserRoleEntity;
import org.temporedata.modules.sys.role.repository.RoleRepository;
import org.temporedata.modules.sys.role.repository.UserRoleRepository;
import org.temporedata.modules.sys.user.entity.UserEntity;
import org.temporedata.modules.sys.user.repository.UserRepository;
import org.temporedata.security.context.TenantContext;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lineage authorization helper. Resolves the current identity (user id, role ids,
 * super admin flag, tenant) from the security/role/tenant contexts and performs
 * VIEW_/EDIT_LINEAGE checks against {@link PermissionService}.
 * <p>Permission model: resourceType = {@code LINEAGE}, action = {@code READ|WRITE},
 * resourceKey = the lineage node id / fqn (wildcard {@code *} grants all, matching the
 * existing RBAC engine). {@code authorizeLineageReference} checks both endpoints of an
 * edge, because a user may only be allowed to see/edit one side.</p>
 */
@Component
@RequiredArgsConstructor
public class LineageAuthHelper {

    // Matches the seeded role code in V1__base_schema.sql (`role-admin` → `ROLE_ADMIN`).
    // Previously this hard-coded `admin`, which never equalled the stored `ROLE_ADMIN`,
    // so the super-admin bypass was dead code and lineage write ops always required an
    // explicit LINEAGE permission even for admins.
    private static final String ADMIN_ROLE_CODE = "ROLE_ADMIN";

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    /** Snapshot of the current caller's identity. */
    @Data
    @AllArgsConstructor
    public static class CurrentIdentity {
        private final String userId;
        private final List<String> roleIds;
        private final boolean superAdmin;
        private final String tenantId;
    }

    public CurrentIdentity current() {
        String userId = currentUserId();
        List<String> roleIds = resolveRoleIds(userId);
        boolean superAdmin = roleIds.stream()
                .map(roleRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(RoleEntity::getCode)
                .anyMatch(ADMIN_ROLE_CODE::equalsIgnoreCase);
        return new CurrentIdentity(userId, roleIds, superAdmin, TenantContext.getTenantId());
    }

    /**
     * Current user id from the authenticated principal.
     * <p>The JwtAuthenticationFilter builds the principal from the {@code User} username,
     * so {@code authentication.getName()} returns the username, not the id. Resolve the
     * real user id via {@link UserRepository} (fallback to the raw name when the user is
     * not found, i.e. the principal name was already an id).</p>
     */
    public String currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userRepository.findByUsername(auth.getName())
                .map(UserEntity::getId)
                .orElse(auth.getName());
    }

    public boolean canRead(String resourceKey) {
        return verify("READ", resourceKey);
    }

    public boolean canEdit(String resourceKey) {
        return verify("WRITE", resourceKey);
    }

    /** Authorize both endpoints of an edge; read path checks READ, write path checks WRITE. */
    public void authorizeLineageReference(String fromId, String toId, boolean write) {
        String action = write ? "WRITE" : "READ";
        boolean fromOk = fromId == null || verify(action, fromId);
        boolean toOk = toId == null || verify(action, toId);
        if (!fromOk || !toOk) {
            throw new org.temporedata.api.base.exceptions.BusinessException(
                    "无权访问血缘节点（缺少 LINEAGE:" + action + " 权限）");
        }
    }

    private boolean verify(String action, String resourceKey) {
        CurrentIdentity id = current();
        if (id.isSuperAdmin()) {
            return true;
        }
        return permissionService.verify(
                id.getRoleIds(), id.isSuperAdmin(), "LINEAGE", action, resourceKey);
    }

    private List<String> resolveRoleIds(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return userRoleRepository.findByIdUserId(userId).stream()
                .map(UserRoleEntity::getId)
                .map(UserRoleEntity.UserRoleId::getRoleId)
                .distinct()
                .collect(Collectors.toList());
    }
}