package org.temporedata.modules.ops.ha.service;

import org.temporedata.modules.ops.ha.entity.HaEntity;
import org.temporedata.modules.ops.ha.repository.HaRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class HaService {
    private final HaRepository haRepository;

    public Page<HaEntity> page(Pageable p) { return haRepository.findAll(p); }

    public List<HaEntity> list() { return haRepository.findAll(); }

    public HaEntity get(String id) { return haRepository.findById(id).orElseThrow(() -> new BusinessException("Ha not found: "+id)); }

    @Transactional
    public HaEntity create(HaEntity e) { return haRepository.save(e); }

    @Transactional
    public HaEntity update(HaEntity e) { return haRepository.save(e); }

    @Transactional
    public void delete(String id) { haRepository.deleteById(id); }
}
