package org.temporedata.modules.sys.view.service;

import org.temporedata.modules.sys.view.entity.ViewEntity;
import org.temporedata.modules.sys.view.repository.ViewRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class ViewService {

    private final ViewRepository viewRepository;

    public List<ViewEntity> list() {
        return viewRepository.findAll();
    }

    public ViewEntity get(String id) {
        return viewRepository.findById(id)
                .orElseThrow(() -> new BusinessException("View not found: " + id));
    }

    @Transactional
    public ViewEntity create(ViewEntity entity) {
        entity.setStatus("DRAFT");
        return viewRepository.save(entity);
    }

    @Transactional
    public ViewEntity update(String id, ViewEntity entity) {
        ViewEntity existing = get(id);
        existing.setName(entity.getName());
        existing.setDescription(entity.getDescription());
        return viewRepository.save(existing);
    }

    @Transactional
    public void delete(String id) {
        viewRepository.deleteById(id);
    }

    @Transactional
    public ViewEntity publish(String id) {
        ViewEntity entity = get(id);
        entity.setStatus("PUBLISHED");
        return viewRepository.save(entity);
    }

    public String execute(String id) {
        ViewEntity entity = get(id);
        log.info("Executing view {}: {}", id, entity.getName());
        return "View executed: " + entity.getName();
    }
}