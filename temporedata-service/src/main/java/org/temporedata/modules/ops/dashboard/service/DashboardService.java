package org.temporedata.modules.ops.dashboard.service;

import org.temporedata.modules.ops.dashboard.entity.DashboardEntity;
import org.temporedata.modules.ops.dashboard.repository.DashboardRepository;
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
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    public Page<DashboardEntity> page(Pageable pageable) { return dashboardRepository.findAll(pageable); }

    public List<DashboardEntity> list() { return dashboardRepository.findAll(); }

    public DashboardEntity get(String id) { return dashboardRepository.findById(id).orElseThrow(() -> new BusinessException("Dashboard not found: " + id)); }

    @Transactional
    public DashboardEntity create(DashboardEntity entity) { return dashboardRepository.save(entity); }

    @Transactional
    public DashboardEntity update(DashboardEntity entity) { return dashboardRepository.save(entity); }

    @Transactional
    public void delete(String id) { dashboardRepository.deleteById(id); }
}
