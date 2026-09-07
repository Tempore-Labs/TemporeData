package org.temporedata.modules.ops.ingestion.service;

import org.temporedata.modules.ops.ingestion.entity.IngestionEntity;
import org.temporedata.modules.ops.ingestion.repository.IngestionRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class IngestionService {

    private final IngestionRepository ingestionRepository;

    /**
     * Paginated list of ingestion tasks.
     */
    public Page<IngestionEntity> page(Pageable pageable) {
        return ingestionRepository.findAll(pageable);
    }

    /**
     * List all ingestion tasks.
     */
    public List<IngestionEntity> list() {
        return ingestionRepository.findAll();
    }

    /**
     * Get ingestion task by id.
     */
    public IngestionEntity get(String id) {
        return ingestionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Ingestion not found: " + id));
    }

    /**
     * Create an ingestion task.
     */
    @Transactional
    public IngestionEntity create(IngestionEntity entity) {
        log.info("Creating ingestion task: name={}", entity.getName());
        return ingestionRepository.save(entity);
    }

    /**
     * Update an existing ingestion task.
     */
    @Transactional
    public IngestionEntity update(String id, IngestionEntity entity) {
        IngestionEntity existing = get(id);
        entity.setId(id);
        entity.setCreateDateTime(existing.getCreateDateTime());
        entity.setCreateBy(existing.getCreateBy());
        entity.setTenantId(existing.getTenantId());
        log.info("Updating ingestion task: id={}, name={}", id, entity.getName());
        return ingestionRepository.save(entity);
    }

    /**
     * Delete an ingestion task by id.
     */
    @Transactional
    public void delete(String id) {
        IngestionEntity entity = get(id);
        ingestionRepository.delete(entity);
        log.info("Deleted ingestion task: id={}", id);
    }

    /**
     * Execute an ingestion task (simulated).
     */
    @Transactional
    public IngestionEntity execute(String id) {
        IngestionEntity entity = get(id);
        entity.setStatus("RUNNING");
        ingestionRepository.save(entity);
        log.info("Executing ingestion task: id={}, name={}", id, entity.getName());

        // Simulate ingestion execution
        try {
            Thread.sleep(100);
            entity.setStatus("SUCCESS");
            entity.setRowCount((entity.getRowCount() != null ? entity.getRowCount() : 0) + 500);
            entity.setLastRunTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            entity.setErrorMsg(null);
            log.info("Ingestion task completed: id={}", id);
        } catch (Exception e) {
            entity.setStatus("FAILED");
            entity.setErrorMsg(e.getMessage());
            log.error("Ingestion task failed: id={}", id, e);
        }
        return ingestionRepository.save(entity);
    }
}