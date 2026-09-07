package org.temporedata.modules.ops.monitor.service;

import org.temporedata.modules.ops.monitor.entity.MonitorEntity;
import org.temporedata.modules.ops.monitor.repository.MonitorRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class MonitorService {
    private final MonitorRepository monitorRepository;

    public Page<MonitorEntity> page(Pageable p) { return monitorRepository.findAll(p); }

    public List<MonitorEntity> list() { return monitorRepository.findAll(); }

    public MonitorEntity get(String id) { return monitorRepository.findById(id).orElseThrow(() -> new BusinessException("Monitor not found: "+id)); }

    @Transactional
    public MonitorEntity create(MonitorEntity e) { return monitorRepository.save(e); }

    @Transactional
    public MonitorEntity update(MonitorEntity e) { return monitorRepository.save(e); }

    @Transactional
    public void delete(String id) { monitorRepository.deleteById(id); }
}
