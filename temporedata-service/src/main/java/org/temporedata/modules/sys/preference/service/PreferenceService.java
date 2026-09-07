package org.temporedata.modules.sys.preference.service;

import org.temporedata.modules.sys.preference.entity.PreferenceEntity;
import org.temporedata.modules.sys.preference.repository.PreferenceRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.temporedata.common.cache.CacheConfig.CACHE_PREFERENCE;

import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class PreferenceService {

    private final PreferenceRepository preferenceRepository;

    @Cacheable(value = CACHE_PREFERENCE)
    public List<PreferenceEntity> getAll() {
        return preferenceRepository.findAll();
    }

    @CacheEvict(value = CACHE_PREFERENCE, allEntries = true)
    @Transactional
    public PreferenceEntity save(PreferenceEntity entity) {
        return preferenceRepository.save(entity);
    }

    @CacheEvict(value = CACHE_PREFERENCE, allEntries = true)
    @Transactional
    public PreferenceEntity update(PreferenceEntity entity) {
        if (entity.getId() == null) {
            throw new BusinessException("Preference id is required for update");
        }
        PreferenceEntity existing = preferenceRepository.findById(entity.getId())
                .orElseThrow(() -> new BusinessException("Preference not found: " + entity.getId()));
        existing.setPrefKey(entity.getPrefKey());
        existing.setPrefValue(entity.getPrefValue());
        existing.setUserId(entity.getUserId());
        existing.setTenantId(entity.getTenantId());
        return preferenceRepository.save(existing);
    }

    @CacheEvict(value = CACHE_PREFERENCE, allEntries = true)
    @Transactional
    public void delete(String id) {
        preferenceRepository.deleteById(id);
    }
}