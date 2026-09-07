package org.temporedata.modules.sys.notify.service;

import org.temporedata.modules.sys.notify.entity.NotifyEntity;
import org.temporedata.modules.sys.notify.repository.NotifyRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor
public class NotifyService {

    private final NotifyRepository notifyRepository;

    public List<NotifyEntity> list(String tenantId) {
        if (tenantId != null && !tenantId.isEmpty()) {
            return notifyRepository.findAll().stream()
                    .filter(n -> tenantId.equals(n.getTenantId()))
                    .collect(Collectors.toList());
        }
        return notifyRepository.findAll();
    }

    public NotifyEntity get(String id) {
        return notifyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Notify not found: " + id));
    }

    @Transactional
    public NotifyEntity create(NotifyEntity entity) {
        return notifyRepository.save(entity);
    }

    @Transactional
    public NotifyEntity update(String id, NotifyEntity entity) {
        NotifyEntity existing = get(id);
        existing.setName(entity.getName());
        existing.setDescription(entity.getDescription());
        existing.setTenantId(entity.getTenantId());
        return notifyRepository.save(existing);
    }

    @Transactional
    public NotifyEntity toggle(String id) {
        NotifyEntity entity = get(id);
        if ("ENABLED".equals(entity.getStatus())) {
            entity.setStatus("DISABLED");
        } else {
            entity.setStatus("ENABLED");
        }
        return notifyRepository.save(entity);
    }

    @Transactional
    public void delete(String id) {
        notifyRepository.deleteById(id);
    }
}