package org.temporedata.modules.ops.sync.service;

import org.temporedata.modules.ops.sync.entity.SyncEntity;
import org.temporedata.modules.ops.sync.repository.SyncRepository;
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
public class SyncService {

    private final SyncRepository syncRepository;

    /**
     * Paginated list of sync tasks.
     */
    public Page<SyncEntity> page(Pageable pageable) {
        return syncRepository.findAll(pageable);
    }

    /**
     * List all sync tasks.
     */
    public List<SyncEntity> list() {
        return syncRepository.findAll();
    }

    /**
     * Get sync task by id.
     */
    public SyncEntity get(String id) {
        return syncRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Sync not found: " + id));
    }

    /**
     * Create a sync task.
     */
    @Transactional
    public SyncEntity create(SyncEntity entity) {
        log.info("Creating sync task: name={}", entity.getName());
        return syncRepository.save(entity);
    }

    /**
     * Update an existing sync task.
     */
    @Transactional
    public SyncEntity update(String id, SyncEntity entity) {
        SyncEntity existing = get(id);
        entity.setId(id);
        entity.setCreateDateTime(existing.getCreateDateTime());
        entity.setCreateBy(existing.getCreateBy());
        entity.setTenantId(existing.getTenantId());
        log.info("Updating sync task: id={}, name={}", id, entity.getName());
        return syncRepository.save(entity);
    }

    /**
     * Delete a sync task by id.
     */
    @Transactional
    public void delete(String id) {
        SyncEntity entity = get(id);
        syncRepository.delete(entity);
        log.info("Deleted sync task: id={}", id);
    }

    /**
     * Execute a sync task (simulated).
     */
    @Transactional
    public SyncEntity execute(String id) {
        SyncEntity entity = get(id);
        entity.setStatus("RUNNING");
        syncRepository.save(entity);
        log.info("Executing sync task: id={}, name={}", id, entity.getName());

        // Simulate sync execution
        try {
            // In real implementation, this would trigger actual data sync logic
            Thread.sleep(100); // simulate brief processing
            entity.setStatus("SUCCESS");
            entity.setRowCount((entity.getRowCount() != null ? entity.getRowCount() : 0) + 100);
            entity.setLastRunTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            entity.setDuration("1.2s");
            entity.setErrorMsg(null);
            log.info("Sync task completed: id={}", id);
        } catch (Exception e) {
            entity.setStatus("FAILED");
            entity.setErrorMsg(e.getMessage());
            log.error("Sync task failed: id={}", id, e);
        }
        return syncRepository.save(entity);
    }
}