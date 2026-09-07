package org.temporedata.modules.ops.container.service;

import org.temporedata.modules.ops.container.entity.ContainerEntity;
import org.temporedata.modules.ops.container.repository.ContainerRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class ContainerService {
    private final ContainerRepository containerRepository;

    public Page<ContainerEntity> page(Pageable p) { return containerRepository.findAll(p); }

    public List<ContainerEntity> list() { return containerRepository.findAll(); }

    public ContainerEntity get(String id) { return containerRepository.findById(id).orElseThrow(() -> new BusinessException("Container not found: "+id)); }

    @Transactional
    public ContainerEntity create(ContainerEntity e) { return containerRepository.save(e); }

    @Transactional
    public ContainerEntity update(ContainerEntity e) { return containerRepository.save(e); }

    @Transactional
    public void delete(String id) { containerRepository.deleteById(id); }
}
