package org.temporedata.modules.dev.schedule.service;

import org.temporedata.modules.dev.schedule.entity.ScheduleEntity;
import org.temporedata.modules.dev.schedule.repository.ScheduleRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    public Page<ScheduleEntity> page(Pageable pageable) { return scheduleRepository.findAll(pageable); }

    public List<ScheduleEntity> list() { return scheduleRepository.findAll(); }

    public ScheduleEntity get(String id) { return scheduleRepository.findById(id).orElseThrow(() -> new BusinessException("Schedule not found: " + id)); }

    @Transactional
    public ScheduleEntity create(ScheduleEntity entity) { return scheduleRepository.save(entity); }

    @Transactional
    public ScheduleEntity update(ScheduleEntity entity) { return scheduleRepository.save(entity); }

    @Transactional
    public void delete(String id) { scheduleRepository.deleteById(id); }
}
