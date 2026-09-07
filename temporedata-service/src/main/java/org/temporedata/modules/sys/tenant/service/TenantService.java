package org.temporedata.modules.sys.tenant.service;

import org.temporedata.modules.sys.tenant.entity.TenantEntity;
import org.temporedata.modules.sys.tenant.repository.TenantRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import static org.temporedata.common.cache.CacheConfig.CACHE_TENANT;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j @Service @RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Cacheable(value = CACHE_TENANT)
    public List<TenantEntity> list() {
        return tenantRepository.findAll();
    }

    @Cacheable(value = CACHE_TENANT)
    public TenantEntity get(String id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Tenant not found: " + id));
    }

    @CacheEvict(value = CACHE_TENANT, allEntries = true)
    @Transactional
    public TenantEntity create(TenantEntity entity) {
        return tenantRepository.save(entity);
    }

    @CacheEvict(value = CACHE_TENANT, allEntries = true)
    @Transactional
    public TenantEntity update(String id, TenantEntity entity) {
        TenantEntity existing = get(id);
        existing.setName(entity.getName());
        existing.setCode(entity.getCode());
        existing.setDescription(entity.getDescription());
        existing.setContactName(entity.getContactName());
        existing.setContactEmail(entity.getContactEmail());
        existing.setStatus(entity.getStatus());
        return tenantRepository.save(existing);
    }

    @CacheEvict(value = CACHE_TENANT, allEntries = true)
    @Transactional
    public void delete(String id) {
        tenantRepository.deleteById(id);
    }

    public List<Map<String, Object>> getMembers(String tenantId) {
        // Placeholder: return tenant members. In a real app, query a user-tenant join table.
        get(tenantId);
        log.info("Fetching members for tenant: {}", tenantId);
        return Collections.emptyList();
    }

    @CacheEvict(value = CACHE_TENANT, allEntries = true)
    @Transactional
    public Map<String, Object> addMember(String tenantId, Map<String, Object> member) {
        get(tenantId);
        log.info("Adding member {} to tenant {}", member, tenantId);
        return member;
    }

    @CacheEvict(value = CACHE_TENANT, allEntries = true)
    @Transactional
    public void removeMember(String tenantId, String userId) {
        get(tenantId);
        log.info("Removing user {} from tenant {}", userId, tenantId);
    }

    @CacheEvict(value = CACHE_TENANT, allEntries = true)
    @Transactional
    public Map<String, Object> setMemberStatus(String tenantId, String userId, Map<String, Object> status) {
        get(tenantId);
        log.info("Setting status for user {} in tenant {}: {}", userId, tenantId, status);
        return status;
    }
}