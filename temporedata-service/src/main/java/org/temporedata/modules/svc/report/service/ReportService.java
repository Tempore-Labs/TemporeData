package org.temporedata.modules.svc.report.service;

import org.temporedata.modules.svc.report.entity.ReportEntity;
import org.temporedata.modules.svc.report.repository.ReportRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.temporedata.common.cache.CacheConfig.CACHE_REPORT;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    @Cacheable(value = CACHE_REPORT)
    public List<ReportEntity> list() {
        return reportRepository.findAll();
    }

    @Cacheable(value = CACHE_REPORT)
    public ReportEntity get(String id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Report not found: " + id));
    }

    @Transactional
    @CacheEvict(value = CACHE_REPORT, allEntries = true)
    public ReportEntity create(ReportEntity entity) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setStatus("DRAFT");
        return reportRepository.save(entity);
    }

    @Transactional
    @CacheEvict(value = CACHE_REPORT, allEntries = true)
    public ReportEntity update(String id, ReportEntity entity) {
        ReportEntity existing = get(id);
        existing.setName(entity.getName());
        existing.setDescription(entity.getDescription());
        existing.setConfig(entity.getConfig());
        existing.setType(entity.getType());
        existing.setDatasourceId(entity.getDatasourceId());
        existing.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return reportRepository.save(existing);
    }

    @Transactional
    @CacheEvict(value = CACHE_REPORT, allEntries = true)
    public void delete(String id) {
        reportRepository.deleteById(id);
    }

    @Transactional
    @CacheEvict(value = CACHE_REPORT, allEntries = true)
    public ReportEntity publish(String id) {
        ReportEntity entity = get(id);
        entity.setStatus("PUBLISHED");
        return reportRepository.save(entity);
    }

    @Transactional
    @CacheEvict(value = CACHE_REPORT, allEntries = true)
    public ReportEntity unpublish(String id) {
        ReportEntity entity = get(id);
        entity.setStatus("DRAFT");
        return reportRepository.save(entity);
    }
}