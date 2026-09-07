package org.temporedata.modules.sys.settings.service;

import org.temporedata.modules.sys.settings.entity.SettingsEntity;
import org.temporedata.modules.sys.settings.repository.SettingsRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.temporedata.common.cache.CacheConfig.CACHE_SETTINGS;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor
public class SettingsService {

    private final SettingsRepository settingsRepository;

    @Cacheable(value = CACHE_SETTINGS)
    public List<SettingsEntity> getAll() {
        return settingsRepository.findAll();
    }

    @Cacheable(value = CACHE_SETTINGS)
    public List<SettingsEntity> getByGroup(String groupName) {
        return settingsRepository.findAll().stream()
                .filter(s -> groupName.equals(s.getName()))
                .collect(Collectors.toList());
    }

    @Cacheable(value = CACHE_SETTINGS)
    public SettingsEntity getByKey(String key) {
        return settingsRepository.findAll().stream()
                .filter(s -> key.equals(s.getName()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Settings not found for key: " + key));
    }

    @CacheEvict(value = CACHE_SETTINGS, allEntries = true)
    @Transactional
    public void saveGroup(String groupName, List<SettingsEntity> settings) {
        settings.forEach(s -> {
            s.setName(groupName);
            settingsRepository.save(s);
        });
    }

    @CacheEvict(value = CACHE_SETTINGS, allEntries = true)
    @Transactional
    public SettingsEntity setValue(String key, SettingsEntity entity) {
        // Try to find existing setting by key (using name field as key)
        java.util.Optional<SettingsEntity> existing = settingsRepository.findAll().stream()
                .filter(s -> key.equals(s.getName()))
                .findFirst();
        if (existing.isPresent()) {
            SettingsEntity e = existing.get();
            e.setDescription(entity.getDescription());
            e.setStatus(entity.getStatus());
            return settingsRepository.save(e);
        }
        entity.setName(key);
        return settingsRepository.save(entity);
    }

    @CacheEvict(value = CACHE_SETTINGS, allEntries = true)
    @Transactional
    public void deleteByKey(String key) {
        settingsRepository.findAll().stream()
                .filter(s -> key.equals(s.getName()))
                .findFirst()
                .ifPresent(s -> settingsRepository.deleteById(s.getId()));
    }
}