package org.temporedata.modules.ops.real.service;

import org.temporedata.modules.ops.real.entity.RealEntity;
import org.temporedata.modules.ops.real.repository.RealRepository;
import org.temporedata.modules.ops.real.EngineLauncher;
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

@Slf4j @Service @RequiredArgsConstructor
public class RealService {

    private final RealRepository realRepository;
    private final EngineLauncher engineLauncher;

    /**
     * Simulated log storage for real-time tasks.
     */
    private final Map<String, List<String>> taskLogs = new HashMap<>();

    /**
     * In-memory record of submitted jobs (taskId -> application/job id), used by stop().
     * Rebuilt on each start.
     */
    private final Map<String, String> runningJobs = new HashMap<>();

    /**
     * Paginated list of real-time tasks.
     */
    public Page<RealEntity> page(Pageable pageable) {
        return realRepository.findAll(pageable);
    }

    /**
     * List all real-time tasks.
     */
    public List<RealEntity> list() {
        return realRepository.findAll();
    }

    /**
     * Get real-time task by id.
     */
    public RealEntity get(String id) {
        return realRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Real-time task not found: " + id));
    }

    /**
     * Create a real-time task.
     */
    @Transactional
    public RealEntity create(RealEntity entity) {
        log.info("Creating real-time task: name={}, type={}", entity.getName(), entity.getType());
        if (entity.getStatus() == null) {
            entity.setStatus("STOPPED");
        }
        return realRepository.save(entity);
    }

    /**
     * Update an existing real-time task.
     */
    @Transactional
    public RealEntity update(String id, RealEntity entity) {
        RealEntity existing = get(id);
        entity.setId(id);
        entity.setCreateDateTime(existing.getCreateDateTime());
        entity.setCreateBy(existing.getCreateBy());
        entity.setTenantId(existing.getTenantId());
        log.info("Updating real-time task: id={}, name={}", id, entity.getName());
        return realRepository.save(entity);
    }

    /**
     * Delete a real-time task by id.
     */
    @Transactional
    public void delete(String id) {
        RealEntity entity = get(id);
        realRepository.delete(entity);
        taskLogs.remove(id);
        log.info("Deleted real-time task: id={}", id);
    }

    /**
     * Submit the real-time task to the real compute engine (Flink / Spark) and, on
     * success, mark it RUNNING with the reported application/job id. Fails honestly
     * (persisting FAILED + throwing) when no engine binary is available or the submit
     * exits non-zero.
     */
    public RealEntity start(String id) {
        RealEntity entity = get(id);
        entity.setStatus("STARTING");
        realRepository.save(entity);
        String now = timestamp();

        try {
            EngineLauncher.SubmitOutcome outcome = engineLauncher.submit(entity);
            entity.setStatus("RUNNING");
            if (outcome.appId() != null) {
                runningJobs.put(id, outcome.appId());
                log.info("Real-time task submitted: id={}, type={}, appId={}", id, entity.getType(), outcome.appId());
            }
            taskLogs.computeIfAbsent(id, k -> new ArrayList<>())
                    .add(now + " [INFO] 已提交计算引擎，application/jobId=" + (outcome.appId() == null ? "?" : outcome.appId()));
            return realRepository.save(entity);
        } catch (BusinessException e) {
            // No surrounding transaction here, so this FAILED save commits even though the
            // exception then bubbles up and the caller sees an error response.
            entity.setStatus("FAILED");
            taskLogs.computeIfAbsent(id, k -> new ArrayList<>()).add(now + " [ERROR] " + e.getMessage());
            realRepository.save(entity);
            runningJobs.remove(id);
            log.warn("Real-time task start failed: id={}, reason={}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * Stop the running job via the engine CLI, then mark the task STOPPED.
     */
    @Transactional
    public RealEntity stop(String id) {
        RealEntity entity = get(id);
        String appId = runningJobs.remove(id);
        try {
            engineLauncher.stop(entity, appId, appId);
            entity.setStatus("STOPPED");
            String now = timestamp();
            taskLogs.computeIfAbsent(id, k -> new ArrayList<>())
                    .add(now + " [INFO] 作业已停止 (appId=" + (appId == null ? "?" : appId) + ")");
            return realRepository.save(entity);
        } catch (BusinessException e) {
            log.warn("Real-time task stop failed: id={}, reason={}", id, e.getMessage());
            throw e;
        }
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Create a savepoint for a real-time task (simulated).
     */
    @Transactional
    public Map<String, Object> savepoint(String id) {
        RealEntity entity = get(id);
        log.info("Creating savepoint for real-time task: id={}, name={}", id, entity.getName());

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String savepointPath = "/tmp/savepoints/" + id + "_" + System.currentTimeMillis();

        taskLogs.computeIfAbsent(id, k -> new ArrayList<>())
                .add(now + " [INFO] Savepoint created at: " + savepointPath);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("savepointPath", savepointPath);
        result.put("createTime", now);
        result.put("status", "SUCCESS");
        return result;
    }

    /**
     * Get logs for a real-time task.
     */
    public List<String> getLogs(String id) {
        // Ensure task exists
        get(id);
        return taskLogs.getOrDefault(id, Collections.singletonList("No logs available."));
    }
}