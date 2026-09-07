package org.temporedata.modules.dev.dependency.service;

import org.temporedata.modules.dev.dependency.entity.DependencyEntity;
import org.temporedata.modules.dev.dependency.repository.DependencyRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class DependencyService {
    private final DependencyRepository dependencyRepository;

    public Page<DependencyEntity> page(Pageable p) { return dependencyRepository.findAll(p); }

    public List<DependencyEntity> list() { return dependencyRepository.findAll(); }

    public DependencyEntity get(String id) { return dependencyRepository.findById(id).orElseThrow(() -> new BusinessException("Dependency not found: "+id)); }

    @Transactional
    public DependencyEntity create(DependencyEntity e) { return dependencyRepository.save(e); }

    @Transactional
    public DependencyEntity update(DependencyEntity e) { return dependencyRepository.save(e); }

    @Transactional
    public void delete(String id) { dependencyRepository.deleteById(id); }
}
