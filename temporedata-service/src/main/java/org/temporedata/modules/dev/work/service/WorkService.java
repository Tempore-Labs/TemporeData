package org.temporedata.modules.dev.work.service;

import org.temporedata.modules.dev.work.entity.WorkEntity;
import org.temporedata.modules.dev.work.repository.WorkRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkService {

    private final WorkRepository workRepository;

    /**
     * List all works.
     */
    public List<WorkEntity> list() {
        return workRepository.findAll();
    }

    /**
     * Paginated work list, optionally filtered by workflowId.
     * Note: WorkEntity currently does not have a workflowId field,
     * so the filter is accepted but not applied.
     */
    public Page<WorkEntity> page(String workflowId, Pageable pageable) {
        if (workflowId != null && !workflowId.isBlank()) {
            log.debug("Filtering by workflowId={} (not yet supported on entity)", workflowId);
        }
        return workRepository.findAll(pageable);
    }

    /**
     * Get work detail by ID.
     */
    public WorkEntity get(String id) {
        return workRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Work not found: " + id));
    }

    /**
     * Add a new work.
     */
    @Transactional
    public WorkEntity add(WorkEntity entity) {
        entity.setStatus(entity.getStatus() != null ? entity.getStatus() : "DRAFT");
        log.info("Adding work: name={}", entity.getName());
        return workRepository.save(entity);
    }

    /**
     * Update an existing work.
     */
    @Transactional
    public WorkEntity update(WorkEntity entity) {
        if (entity.getId() == null) {
            throw new BusinessException("Work id is required for update");
        }
        get(entity.getId()); // ensure exists
        log.info("Updating work: id={}, name={}", entity.getId(), entity.getName());
        return workRepository.save(entity);
    }

    /**
     * Delete a work by ID.
     */
    @Transactional
    public void delete(String workId) {
        get(workId);
        workRepository.deleteById(workId);
        log.info("Deleted work: id={}", workId);
    }

    /**
     * Run / execute a work.
     */
    @Transactional
    public WorkEntity run(String workId) {
        WorkEntity entity = get(workId);
        entity.setStatus("RUNNING");
        log.info("Running work: id={}, name={}", workId, entity.getName());
        return workRepository.save(entity);
    }

    /**
     * Stop a running work.
     */
    @Transactional
    public WorkEntity stop(String workId) {
        WorkEntity entity = get(workId);
        entity.setStatus("STOPPED");
        log.info("Stopping work: id={}, name={}", workId, entity.getName());
        return workRepository.save(entity);
    }

    /**
     * Copy a work by creating a duplicate with a new name.
     */
    @Transactional
    public WorkEntity copy(String workId) {
        WorkEntity source = get(workId);
        WorkEntity copy = WorkEntity.builder()
                .name(source.getName() + " (Copy)")
                .status("DRAFT")
                .description(source.getDescription())
                .build();
        log.info("Copying work: from={}, to={}", workId, copy.getName());
        return workRepository.save(copy);
    }

    /**
     * Top / pin a work (simulated by updating the update timestamp).
     */
    @Transactional
    public WorkEntity top(String workId) {
        WorkEntity entity = get(workId);
        // No explicit "top" field on entity; simulate by updating the entity
        log.info("Topping work: id={}, name={}", workId, entity.getName());
        return workRepository.save(entity);
    }

    /**
     * Get current status of a work.
     */
    public Map<String, Object> status(String workId) {
        WorkEntity entity = get(workId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workId", entity.getId());
        result.put("name", entity.getName());
        result.put("status", entity.getStatus());
        result.put("description", entity.getDescription());
        return result;
    }

    /**
     * Get instance list for a work (simulated).
     */
    public List<Map<String, Object>> instances(String workId) {
        WorkEntity entity = get(workId);
        List<Map<String, Object>> instances = new ArrayList<>();

        // Simulated instance records
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> inst = new LinkedHashMap<>();
            inst.put("instanceId", workId + "-inst-" + i);
            inst.put("workId", workId);
            inst.put("workName", entity.getName());
            inst.put("status", i == 1 ? "SUCCESS" : (i == 2 ? "RUNNING" : "PENDING"));
            inst.put("startTime", LocalDateTime.now().minusHours(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            inst.put("durationMs", (long) (Math.random() * 5000));
            instances.add(inst);
        }

        return instances;
    }
}