package org.temporedata.modules.ops.alarm.service;

import org.temporedata.modules.ops.alarm.entity.AlarmEntity;
import org.temporedata.modules.ops.alarm.repository.AlarmRepository;
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
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor
public class AlarmService {

    private final AlarmRepository alarmRepository;

    /**
     * In-memory store for alarm records (simulated).
     * In production, this would be a separate table/entity.
     */
    private final Map<String, List<Map<String, Object>>> alarmRecordsStore = new HashMap<>();

    /**
     * Paginated list of alarm configs.
     */
    public Page<AlarmEntity> page(Pageable pageable) {
        return alarmRepository.findAll(pageable);
    }

    /**
     * List all alarm configs.
     */
    public List<AlarmEntity> list() {
        return alarmRepository.findAll();
    }

    /**
     * Get alarm config by id.
     */
    public AlarmEntity get(String id) {
        return alarmRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Alarm config not found: " + id));
    }

    /**
     * Create an alarm config.
     */
    @Transactional
    public AlarmEntity create(AlarmEntity entity) {
        log.info("Creating alarm config: name={}, eventType={}", entity.getName(), entity.getEventType());
        return alarmRepository.save(entity);
    }

    /**
     * Update an existing alarm config.
     */
    @Transactional
    public AlarmEntity update(String id, AlarmEntity entity) {
        AlarmEntity existing = get(id);
        entity.setId(id);
        entity.setCreateDateTime(existing.getCreateDateTime());
        entity.setCreateBy(existing.getCreateBy());
        entity.setTenantId(existing.getTenantId());
        log.info("Updating alarm config: id={}, name={}", id, entity.getName());
        return alarmRepository.save(entity);
    }

    /**
     * Delete an alarm config by id.
     */
    @Transactional
    public void delete(String id) {
        AlarmEntity entity = get(id);
        alarmRepository.delete(entity);
        alarmRecordsStore.remove(id);
        log.info("Deleted alarm config: id={}", id);
    }

    /**
     * Get alarm records for a given config (simulated).
     */
    public List<Map<String, Object>> getRecords(String configId) {
        // Ensure config exists
        get(configId);
        return alarmRecordsStore.getOrDefault(configId, new ArrayList<>());
    }

    /**
     * Test an alarm config by simulating a notification.
     */
    @Transactional
    public Map<String, Object> test(String id) {
        AlarmEntity entity = get(id);
        log.info("Testing alarm config: id={}, name={}", id, entity.getName());

        // Simulate sending alarm
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", UUID.randomUUID().toString().replace("-", ""));
        record.put("configId", id);
        record.put("message", "Test alarm: " + entity.getName());
        record.put("status", "SENT");
        record.put("sendTime", now);

        // Store the record
        alarmRecordsStore.computeIfAbsent(id, k -> new ArrayList<>()).add(record);

        // Update entity status
        entity.setStatus("SENT");
        entity.setSendTime(now);
        alarmRepository.save(entity);

        log.info("Alarm test completed: id={}", id);
        return record;
    }
}