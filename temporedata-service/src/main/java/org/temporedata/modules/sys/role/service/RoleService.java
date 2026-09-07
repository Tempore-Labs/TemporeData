package org.temporedata.modules.sys.role.service;

import org.temporedata.modules.sys.role.entity.RoleEntity;
import org.temporedata.modules.sys.role.entity.UserRoleEntity;
import org.temporedata.modules.sys.role.repository.RoleRepository;
import org.temporedata.modules.sys.role.repository.UserRoleRepository;
import org.temporedata.modules.sys.user.entity.UserEntity;
import org.temporedata.modules.sys.user.repository.UserRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.temporedata.common.cache.CacheConfig.CACHE_ROLE;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j @Service @RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;

    @Cacheable(value = CACHE_ROLE)
    public List<RoleEntity> list() {
        List<RoleEntity> roles = roleRepository.findAll();
        // Flag built-in roles (seeded with ROLE_ codes) as protected.
        for (RoleEntity r : roles) {
            if (r.getCode() != null && r.getCode().startsWith("ROLE_")) {
                r.setProtectedRole(Boolean.TRUE);
            }
        }
        return roles;
    }

    @Cacheable(value = CACHE_ROLE)
    public RoleEntity get(String id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Role not found: " + id));
    }

    @CacheEvict(value = CACHE_ROLE, allEntries = true)
    @Transactional
    public RoleEntity create(RoleEntity entity) {
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            throw new BusinessException("角色编码必填");
        }
        if (entity.getName() == null || entity.getName().isBlank()) {
            throw new BusinessException("角色名称必填");
        }
        entity.setId(null);
        return roleRepository.save(entity);
    }

    @CacheEvict(value = CACHE_ROLE, allEntries = true)
    @Transactional
    public RoleEntity update(String id, RoleEntity entity) {
        RoleEntity existing = get(id);
        if (entity.getName() != null) existing.setName(entity.getName());
        if (entity.getCode() != null && !entity.getCode().isBlank()) existing.setCode(entity.getCode());
        if (entity.getRemark() != null) existing.setRemark(entity.getRemark());
        return roleRepository.save(existing);
    }

    @CacheEvict(value = CACHE_ROLE, allEntries = true)
    @Transactional
    public void delete(String id) {
        roleRepository.deleteById(id);
    }

    /**
     * Resolve the member usernames for a role by joining zy_user_role with zy_user.
     */
    @Cacheable(value = CACHE_ROLE)
    public List<Map<String, Object>> getMembers(String id) {
        RoleEntity role = get(id);
        List<UserRoleEntity> links = userRoleRepository.findByIdRoleId(role.getId());
        List<Map<String, Object>> members = new ArrayList<>();
        for (UserRoleEntity link : links) {
            String uid = link.getId().getUserId();
            userRepository.findById(uid).ifPresent(u -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("userId", u.getId());
                m.put("username", u.getUsername());
                m.put("tenantName", u.getTenantId());
                m.put("createTime", u.getCreatedAt());
                members.add(m);
            });
        }
        return members;
    }
}