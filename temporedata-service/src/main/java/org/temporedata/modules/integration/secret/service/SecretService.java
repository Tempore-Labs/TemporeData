package org.temporedata.modules.integration.secret.service;

import org.temporedata.api.integration.secret.SecretReq;
import org.temporedata.api.integration.secret.SecretRes;
import org.temporedata.modules.integration.secret.entity.SecretEntity;
import org.temporedata.modules.integration.secret.repository.SecretRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor
public class SecretService {

    private final SecretRepository secretRepository;

    /** Safe list of secrets (DTO contract, v2.0 §6.3). */
    public List<SecretRes> listRes() {
        return secretRepository.findAll().stream().map(this::toRes).collect(Collectors.toList());
    }

    public SecretRes getRes(String id) {
        return toRes(get(id));
    }

    @Transactional
    public SecretRes create(SecretReq req) {
        SecretEntity entity = SecretEntity.builder()
                .key(req.getKey())
                .value(req.getValue())
                .description(req.getDescription())
                .scope(req.getScope())
                .createTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
        return toRes(secretRepository.save(entity));
    }

    @Transactional
    public SecretRes update(String id, SecretReq req) {
        SecretEntity existing = get(id);
        existing.setKey(req.getKey());
        existing.setValue(req.getValue());
        existing.setDescription(req.getDescription());
        existing.setScope(req.getScope());
        return toRes(secretRepository.save(existing));
    }

    @Transactional
    public void delete(String id) {
        secretRepository.deleteById(id);
    }

    /** Internal helper: existing entity access. */
    private SecretEntity get(String id) {
        return secretRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Secret not found: " + id));
    }

    /** Map entity to its external contract (omits createBy/updateBy/tenantId etc.). */
    private SecretRes toRes(SecretEntity e) {
        SecretRes r = new SecretRes();
        r.setId(e.getId());
        r.setKey(e.getKey());
        r.setValue(e.getValue());
        r.setDescription(e.getDescription());
        r.setScope(e.getScope());
        r.setCreateTime(e.getCreateTime());
        return r;
    }
}