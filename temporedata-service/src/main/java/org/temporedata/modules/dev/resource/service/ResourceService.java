package org.temporedata.modules.dev.resource.service;

import org.temporedata.modules.dev.resource.entity.ResourceEntity;
import org.temporedata.modules.dev.resource.repository.ResourceRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class ResourceService {
    private final ResourceRepository resourceRepository;

    public Page<ResourceEntity> page(Pageable p) { return resourceRepository.findAll(p); }

    public List<ResourceEntity> list() { return resourceRepository.findAll(); }

    public ResourceEntity get(String id) { return resourceRepository.findById(id).orElseThrow(() -> new BusinessException("Resource not found: "+id)); }

    @Transactional
    public ResourceEntity create(ResourceEntity e) { return resourceRepository.save(e); }

    @Transactional
    public ResourceEntity update(ResourceEntity e) { return resourceRepository.save(e); }

    @Transactional
    public void delete(String id) { resourceRepository.deleteById(id); }
}
