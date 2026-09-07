package org.temporedata.modules.sys.user.service;

import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.sys.account.ChangePasswordReq;
import org.temporedata.api.sys.account.ContactUpdateReq;
import org.temporedata.api.sys.account.UpdateProfileReq;
import org.temporedata.api.sys.auth.UserInfoRes;
import org.temporedata.api.sys.menu.MenuGroup;
import org.temporedata.api.sys.user.UserRes;
import org.temporedata.modules.dev.perm.service.PermissionService;
import org.temporedata.modules.sys.menu.MenuRegistry;
import org.temporedata.modules.sys.role.entity.RoleEntity;
import org.temporedata.modules.sys.role.entity.UserRoleEntity;
import org.temporedata.modules.sys.role.repository.RoleRepository;
import org.temporedata.modules.sys.role.repository.UserRoleRepository;
import org.temporedata.modules.sys.user.entity.UserEntity;
import org.temporedata.modules.sys.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.temporedata.common.cache.CacheConfig.CACHE_USER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor
public class UserService {

    private static final String ADMIN_ROLE = "admin";
    private static final String PERMISSION_PREFIX = "permission:";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PermissionService permissionService;
    private final MenuRegistry menuRegistry;
    private final PasswordEncoder passwordEncoder;

    @Cacheable(value = CACHE_USER)
    public List<UserEntity> list() {
        return userRepository.findAll();
    }

    @Cacheable(value = CACHE_USER)
    public UserEntity get(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在: " + id));
    }

    /** Safe list of users (DTO, no password). */
    public List<UserRes> listRes() {
        return list().stream().map(this::toRes).collect(Collectors.toList());
    }

    /** Safe single user (DTO, no password). */
    public UserRes getRes(String id) {
        return toRes(get(id));
    }

    /** Map an entity to its external contract, never exposing the password. */
    public UserRes toRes(UserEntity u) {
        return UserRes.builder()
                .id(u.getId())
                .username(u.getUsername())
                .tenantId(u.getTenantId())
                .status(u.getStatus())
                .density(u.getDensity())
                .mustChangePassword(u.getMustChangePassword())
                .nickname(u.getNickname())
                .phone(u.getPhone())
                .email(u.getEmail())
                .createdAt(u.getCreatedAt())
                .build();
    }

    /**
     * Identity for the currently authenticated user (safe, password never leaked).
     * Returns roles + permission points + super-admin flag so the frontend can
     * render a permission-driven sidebar (v2.0 §19).
     */
    public UserInfoRes me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("当前用户不存在: " + username));
        return buildUserInfo(user);
    }

    /** Permission-driven navigation for the current user (v2.0 §19). */
    public List<MenuGroup> menus() {
        UserInfoRes info = me();
        Set<String> resourceTypes = new HashSet<>(info.getPermissions() == null
                ? List.of() : info.getPermissions());
        return menuRegistry.menusFor(info.isAdmin(), resourceTypes);
    }

    /**
     * Build the frontend-facing identity (roles, permissions, super-admin) for a user.
     */
    private UserInfoRes buildUserInfo(UserEntity user) {
        List<String> roleIds = resolveRoleIds(user.getId());
        List<RoleEntity> roles = roleIds.stream()
                .map(roleRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());
        List<String> roleNames = roles.stream().map(RoleEntity::getCode).collect(Collectors.toList());
        boolean superAdmin = roles.stream().anyMatch(r -> ADMIN_ROLE.equalsIgnoreCase(r.getCode()));
        List<String> permissions = permissionService.myPerms(roleIds);

        UserInfoRes info = new UserInfoRes();
        info.setUserId(user.getId());
        info.setUsername(user.getUsername());
        info.setTenantId(user.getTenantId());
        info.setTenantName(user.getTenantId());
        info.setRoles(roleNames);
        info.setPermissions(permissions);
        info.setAdmin(superAdmin);
        info.setNickname(user.getNickname());
        info.setPhone(user.getPhone());
        info.setEmail(user.getEmail());
        return info;
    }

    @CacheEvict(value = CACHE_USER, allEntries = true)
    @Transactional
    public UserEntity create(UserEntity entity) {
        if (entity.getUsername() == null || entity.getUsername().isBlank()) {
            throw new BusinessException("用户名必填");
        }
        if (entity.getPassword() == null || entity.getPassword().isBlank()) {
            throw new BusinessException("密码必填");
        }
        entity.setId(null);
        // passwords MUST be stored encrypted (project hard constraint)
        entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        if (entity.getStatus() == null) entity.setStatus(1);
        if (entity.getTenantId() == null) entity.setTenantId("DEFAULT");
        return userRepository.save(entity);
    }

    @CacheEvict(value = CACHE_USER, allEntries = true)
    @Transactional
    public UserEntity update(String id, UserEntity entity) {
        UserEntity existing = get(id);
        if (entity.getUsername() != null) existing.setUsername(entity.getUsername());
        if (entity.getPassword() != null && !entity.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(entity.getPassword()));
        }
        if (entity.getDensity() != null) existing.setDensity(entity.getDensity());
        if (entity.getMustChangePassword() != null) existing.setMustChangePassword(entity.getMustChangePassword());
        if (entity.getStatus() != null) existing.setStatus(entity.getStatus());
        if (entity.getTenantId() != null) existing.setTenantId(entity.getTenantId());
        return userRepository.save(existing);
    }

    @CacheEvict(value = CACHE_USER, allEntries = true)
    @Transactional
    public void delete(String id) {
        userRepository.deleteById(id);
        userRoleRepository.deleteByIdUserId(id);
    }

    /**
     * Replace the roles bound to a user (P4-SYS 4.3 identity <-> authorization).
     */
    @CacheEvict(value = CACHE_USER, allEntries = true)
    @Transactional
    public void setRoles(String userId, List<String> roleIds) {
        get(userId);
        userRoleRepository.deleteByIdUserId(userId);
        if (roleIds == null) return;
        for (String roleId : roleIds) {
            if (roleId == null || roleId.isBlank()) continue;
            UserRoleEntity link = UserRoleEntity.builder()
                    .id(UserRoleEntity.UserRoleId.builder()
                            .userId(userId).roleId(roleId).build())
                    .build();
            userRoleRepository.save(link);
        }
    }

    public Map<String, Object> ping() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /** The currently authenticated user entity (resolved by name from the JWT). */
    private UserEntity currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("当前用户不存在: " + username));
    }

    /** Update own basic profile (nickname / phone / email). */
    @CacheEvict(value = CACHE_USER, allEntries = true)
    @Transactional
    public UserRes updateProfile(UpdateProfileReq req) {
        UserEntity user = currentUser();
        if (req.getNickname() != null) user.setNickname(req.getNickname());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        return toRes(userRepository.save(user));
    }

    /** Self-service password change: verifies the old password, then hashes the new one. */
    @CacheEvict(value = CACHE_USER, allEntries = true)
    @Transactional
    public void changePassword(ChangePasswordReq req) {
        if (req.getNewPassword() == null || req.getNewPassword().length() < 6) {
            throw new BusinessException("新密码长度至少6位");
        }
        UserEntity user = currentUser();
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        log.info("User {} changed password", user.getUsername());
    }

    /** Change own phone number. */
    @CacheEvict(value = CACHE_USER, allEntries = true)
    @Transactional
    public UserRes changePhone(ContactUpdateReq req) {
        if (req.getValue() == null || req.getValue().isBlank()) {
            throw new BusinessException("手机号不能为空");
        }
        UserEntity user = currentUser();
        user.setPhone(req.getValue());
        return toRes(userRepository.save(user));
    }

    /** Change own email address. */
    @CacheEvict(value = CACHE_USER, allEntries = true)
    @Transactional
    public UserRes changeEmail(ContactUpdateReq req) {
        if (req.getValue() == null || req.getValue().isBlank()) {
            throw new BusinessException("邮箱不能为空");
        }
        UserEntity user = currentUser();
        user.setEmail(req.getValue());
        return toRes(userRepository.save(user));
    }

    private List<String> resolveRoleIds(String userId) {
        List<String> ids = new ArrayList<>();
        for (UserRoleEntity link : userRoleRepository.findByIdUserId(userId)) {
            if (link.getId() != null && link.getId().getRoleId() != null) {
                ids.add(link.getId().getRoleId());
            }
        }
        return ids;
    }
}