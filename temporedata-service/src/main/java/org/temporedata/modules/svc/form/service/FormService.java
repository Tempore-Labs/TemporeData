package org.temporedata.modules.svc.form.service;

import org.temporedata.modules.svc.form.entity.FormEntity;
import org.temporedata.modules.svc.form.entity.FormSubmissionEntity;
import org.temporedata.modules.svc.form.repository.FormRepository;
import org.temporedata.modules.svc.form.repository.FormSubmissionRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.temporedata.common.cache.CacheConfig.CACHE_FORM;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j @Service @RequiredArgsConstructor
public class FormService {

    private final FormRepository formRepository;
    private final FormSubmissionRepository formSubmissionRepository;

    @Cacheable(value = CACHE_FORM)
    public List<FormEntity> list() {
        return formRepository.findAll();
    }

    @Cacheable(value = CACHE_FORM)
    public FormEntity get(String id) {
        return formRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Form not found: " + id));
    }

    @Transactional
    @CacheEvict(value = CACHE_FORM, allEntries = true)
    public FormEntity create(FormEntity entity) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setStatus("DRAFT");
        return formRepository.save(entity);
    }

    @Transactional
    @CacheEvict(value = CACHE_FORM, allEntries = true)
    public FormEntity update(String id, FormEntity entity) {
        FormEntity existing = get(id);
        existing.setName(entity.getName());
        existing.setDescription(entity.getDescription());
        existing.setConfig(entity.getConfig());
        existing.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return formRepository.save(existing);
    }

    @Transactional
    @CacheEvict(value = CACHE_FORM, allEntries = true)
    public void delete(String id) {
        formRepository.deleteById(id);
    }

    @Cacheable(value = CACHE_FORM)
    public List<FormSubmissionEntity> getSubmissions(String formId) {
        return formSubmissionRepository.findByFormId(formId);
    }

    @Transactional
    @CacheEvict(value = CACHE_FORM, allEntries = true)
    public String generateShareToken(String id) {
        FormEntity entity = get(id);
        String token = UUID.randomUUID().toString().replace("-", "");
        entity.setShareToken(token);
        formRepository.save(entity);
        return token;
    }
}