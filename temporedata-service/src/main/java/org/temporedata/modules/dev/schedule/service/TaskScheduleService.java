package org.temporedata.modules.dev.schedule.service;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.dev.schedule.*;
import org.temporedata.modules.dev.schedule.calendar.BizDateCalculator;
import org.temporedata.modules.dev.schedule.entity.TaskDefineEntity;
import org.temporedata.modules.dev.schedule.entity.TaskInstanceEntity;
import org.temporedata.modules.dev.schedule.entity.TaskLogEntity;
import org.temporedata.modules.dev.schedule.repository.TaskDefineRepository;
import org.temporedata.modules.dev.schedule.repository.TaskInstanceRepository;
import org.temporedata.modules.dev.schedule.repository.TaskLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskScheduleService {

    private final TaskDefineRepository taskDefineRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final TaskLogRepository taskLogRepository;
    private final TaskDispatcher taskDispatcher;
    private final BizDateCalculator bizDateCalculator;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ---- Constant task meta ----
    private static final String STATUS_NORMAL = "NORMAL";

    /** Hour used to derive a deterministic trigger time per backfilled biz date. */
    private static final int DEFAULT_BACKFILL_TRIGGER_HOUR = 8;

    // ---- Task definitions ----

    @Transactional(readOnly = true)
    public List<TaskDefineRes> list() {
        return taskDefineRepository.findAll().stream()
                .map(this::toDefineRes)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskDefineRes get(String id) {
        return toDefineRes(findDefine(id));
    }

    @Transactional
    public TaskDefineRes create(TaskDefineReq req) {
        validate(req);
        TaskDefineEntity entity = TaskDefineEntity.builder()
                .name(req.getName())
                .taskType(req.getTaskType() == null || req.getTaskType().isBlank() ? "WORKFLOW" : req.getTaskType().toUpperCase())
                .targetRef(req.getTargetRef())
                .cronExpression(req.getCronExpression())
                .bizDateMode(req.getBizDateMode() == null ? "NONE" : req.getBizDateMode())
                .calendarId(req.getCalendarId())
                .timezone(req.getTimezone() == null || req.getTimezone().isBlank() ? "Asia/Shanghai" : req.getTimezone())
                .enabled(req.getEnabled() == null || req.getEnabled())
                .paramsJson(req.getParams())
                .owner(req.getOwner())
                .status(STATUS_NORMAL)
                .version(0)
                .build();
        return toDefineRes(taskDefineRepository.save(entity));
    }

    @Transactional
    public TaskDefineRes update(String id, TaskDefineReq req) {
        TaskDefineEntity entity = findDefine(id);
        if (req.getCronExpression() != null) {
            validateCron(req.getCronExpression());
            entity.setCronExpression(req.getCronExpression());
        }
        if (req.getName() != null) entity.setName(req.getName());
        if (req.getTaskType() != null) entity.setTaskType(req.getTaskType().toUpperCase());
        if (req.getTargetRef() != null) entity.setTargetRef(req.getTargetRef());
        if (req.getBizDateMode() != null) entity.setBizDateMode(req.getBizDateMode());
        if (req.getCalendarId() != null) entity.setCalendarId(req.getCalendarId());
        if (req.getTimezone() != null) entity.setTimezone(req.getTimezone());
        if (req.getParams() != null) entity.setParamsJson(req.getParams());
        if (req.getOwner() != null) entity.setOwner(req.getOwner());
        if (req.getEnabled() != null) entity.setEnabled(req.getEnabled());
        entity.setVersion(entity.getVersion() + 1);
        return toDefineRes(taskDefineRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        taskDefineRepository.deleteById(id);
    }

    @Transactional
    public TaskDefineRes setEnabled(String id, boolean enabled) {
        TaskDefineEntity entity = findDefine(id);
        entity.setEnabled(enabled);
        entity.setStatus(STATUS_NORMAL);
        return toDefineRes(taskDefineRepository.save(entity));
    }

    // ---- Instances & logs ----

    @Transactional(readOnly = true)
    public List<TaskInstanceRes> instances(String taskId) {
        return taskInstanceRepository.findByTaskIdOrderByTriggerTimeDesc(taskId).stream()
                .map(this::toInstanceRes)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskLogRes> logs(String instanceId) {
        return taskLogRepository.findByInstanceIdOrderByCreatedAtAsc(instanceId).stream()
                .map(this::toLogRes)
                .collect(Collectors.toList());
    }

    // ---- Trigger ----

    /**
     * Manually trigger a task now (PENDING -> RUNNING -> terminal).
     */
    @Transactional
    public TaskInstanceRes manualTrigger(String taskId) {
        TaskDefineEntity task = findDefine(taskId);
        fire(task, LocalDateTime.now());
        TaskInstanceEntity latest = taskInstanceRepository
                .findTopByTaskIdOrderByInstanceNoDesc(taskId)
                .orElseThrow(() -> new BusinessException("Task instance not found after trigger"));
        return toInstanceRes(latest);
    }

    /**
     * Create (if absent) an instance for <code>trigger</code> and run it.
     * Used by the scheduler coordinator and manual trigger. Idempotent on
     * (task_id, trigger_time).
     *
     * @return false if an instance for this trigger time already exists
     */
    @Transactional
    public boolean fire(TaskDefineEntity task, LocalDateTime trigger) {
        String taskId = task.getId();
        if (taskInstanceRepository.existsByTaskIdAndTriggerTime(taskId, trigger)) {
            log.debug("Skip duplicate trigger {} for task {}", trigger, taskId);
            return false;
        }

        long nextNo = taskInstanceRepository.findTopByTaskIdOrderByInstanceNoDesc(taskId)
                .map(inst -> inst.getInstanceNo() + 1)
                .orElse(1L);

        String bizDate = bizDateCalculator.computeBizDate(task,
                ZonedDateTime.now(toZone(task.getTimezone())));

        TaskInstanceEntity instance = TaskInstanceEntity.builder()
                .taskId(taskId)
                .instanceNo(nextNo)
                .triggerTime(trigger)
                .bizDate(bizDate)
                .status("RUNNING")
                .startTime(LocalDateTime.now())
                .version(0)
                .build();
        taskInstanceRepository.save(instance);
        logInstance(instance, "INFO", "Instance triggered by schedule: " + trigger.format(DTF)
                + (bizDate == null ? "" : ", bizDate=" + bizDate));

        runInstance(task, instance);
        return true;
    }

    /**
     * Fire an instance for a fixed business date (used by backfill). Idempotent
     * on (task_id, biz_date).
     */
    @Transactional
    public boolean fireForBizDate(TaskDefineEntity task, String bizDate) {
        String taskId = task.getId();
        if (taskInstanceRepository.existsByTaskIdAndBizDate(taskId, bizDate)) {
            log.debug("Instance already exists for task {} bizDate {}",
                    taskId, bizDate);
            return false;
        }
        long nextNo = taskInstanceRepository.findTopByTaskIdOrderByInstanceNoDesc(taskId)
                .map(inst -> inst.getInstanceNo() + 1)
                .orElse(1L);

        // Derive triggerTime deterministically from the business date instead of
        // LocalDateTime.now() so a backfill spanning multiple days never collides
        // on the (task_id, trigger_time) unique key within the same second. The
        // stored value is truncated to second precision by the DATETIME column.
        LocalDateTime triggerTime = LocalDate.parse(bizDate).atTime(DEFAULT_BACKFILL_TRIGGER_HOUR, 0);

        TaskInstanceEntity instance = TaskInstanceEntity.builder()
                .taskId(taskId)
                .instanceNo(nextNo)
                .triggerTime(triggerTime)
                .bizDate(bizDate)
                .status("RUNNING")
                .startTime(LocalDateTime.now())
                .version(0)
                .build();
        taskInstanceRepository.save(instance);
        logInstance(instance, "INFO", "Instance backfilled for bizDate=" + bizDate);

        runInstance(task, instance);
        return true;
    }

    private void runInstance(TaskDefineEntity task, TaskInstanceEntity instance) {
        TaskDispatchResult result;
        try {
            result = taskDispatcher.dispatch(task, instance);
        } catch (Exception e) {
            log.error("Dispatch threw exception for task {}", task.getId(), e);
            result = TaskDispatchResult.fail("Dispatch error: " + e.getMessage());
        }
        boolean success = result.isSuccess();
        instance.setStatus(success ? "SUCCESS" : "FAILED");
        instance.setFinishTime(LocalDateTime.now());
        instance.setResultMsg(result.getMessage());
        instance.setVersion(instance.getVersion() + 1);
        taskInstanceRepository.save(instance);
        logInstance(instance, success ? "INFO" : "ERROR", result.getMessage());
    }

    // ---- Business date preview & backfill (P1-4) ----

    @Transactional(readOnly = true)
    public List<BizDatePreviewItem> bizDatePreview(String taskId, LocalDate from, LocalDate to) {
        TaskDefineEntity task = findDefine(taskId);
        ZoneId zone = toZone(task.getTimezone());
        Set<String> seen = new LinkedHashSet<>();
        List<BizDatePreviewItem> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            // Simulate a trigger on this day after the cut (08:00 default) to derive biz_date.
            String biz = bizDateCalculator.computeBizDate(task, ZonedDateTime.of(d, LocalTime.of(8, 0), zone));
            if (biz == null || !seen.add(biz)) continue;
            BizDatePreviewItem item = new BizDatePreviewItem();
            item.setBizDate(biz);
            item.setWorkday(bizDateCalculator.isWorkday(task.getCalendarId(), LocalDate.parse(biz)));
            item.setTriggerTime(d.toString() + " 08:00:00");
            result.add(item);
        }
        return result;
    }

    @Transactional
    public int backfill(String taskId, LocalDate from, LocalDate to) {
        TaskDefineEntity task = findDefine(taskId);
        List<BizDatePreviewItem> preview = bizDatePreview(taskId, from, to);
        int count = 0;
        for (BizDatePreviewItem item : preview) {
            if (fireForBizDate(task, item.getBizDate())) count++;
        }
        log.info("Backfilled {} instance(s) for task {} in [{}, {}]", count, taskId, from, to);
        return count;
    }

    private ZoneId toZone(String tz) {
        try {
            return tz == null || tz.isBlank() ? ZoneId.systemDefault() : ZoneId.of(tz);
        } catch (Exception e) {
            return ZoneId.systemDefault();
        }
    }

    private void logInstance(TaskInstanceEntity instance, String level, String message) {
        try {
            taskLogRepository.save(TaskLogEntity.builder()
                    .instanceId(instance.getId())
                    .level(level)
                    .message(message)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to persist task log for instance {}", instance.getId(), e);
        }
    }

    // ---- Helpers ----

    private TaskDefineEntity findDefine(String id) {
        return taskDefineRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Task definition not found: " + id));
    }

    private void validate(TaskDefineReq req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BusinessException("Task name is required");
        }
        if (req.getCronExpression() == null || req.getCronExpression().isBlank()) {
            throw new BusinessException("Cron expression is required");
        }
        validateCron(req.getCronExpression());
    }

    private void validateCron(String expr) {
        try {
            CronDefinition cd = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
            new CronParser(cd).parse(expr);
        } catch (Exception e) {
            throw new BusinessException("Invalid cron expression: " + expr);
        }
    }

    private TaskDefineRes toDefineRes(TaskDefineEntity e) {
        TaskDefineRes r = new TaskDefineRes();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setTaskType(e.getTaskType());
        r.setTargetRef(e.getTargetRef());
        r.setCronExpression(e.getCronExpression());
        r.setBizDateMode(e.getBizDateMode());
        r.setCalendarId(e.getCalendarId());
        r.setTimezone(e.getTimezone());
        r.setEnabled(e.getEnabled());
        r.setParams(e.getParamsJson());
        r.setOwner(e.getOwner());
        r.setStatus(e.getStatus());
        r.setCreateTime(e.getCreatedAt() != null ? e.getCreatedAt().format(DTF) : null);
        r.setUpdateTime(e.getUpdatedAt() != null ? e.getUpdatedAt().format(DTF) : null);
        return r;
    }

    private TaskInstanceRes toInstanceRes(TaskInstanceEntity e) {
        TaskInstanceRes r = new TaskInstanceRes();
        r.setId(e.getId());
        r.setTaskId(e.getTaskId());
        r.setInstanceNo(e.getInstanceNo());
        r.setTriggerTime(e.getTriggerTime() != null ? e.getTriggerTime().format(DTF) : null);
        r.setStartTime(e.getStartTime() != null ? e.getStartTime().format(DTF) : null);
        r.setFinishTime(e.getFinishTime() != null ? e.getFinishTime().format(DTF) : null);
        r.setBizDate(e.getBizDate());
        r.setStatus(e.getStatus());
        r.setTriggerNode(e.getTriggerNode());
        r.setResultMsg(e.getResultMsg());
        return r;
    }

    private TaskLogRes toLogRes(TaskLogEntity e) {
        TaskLogRes r = new TaskLogRes();
        r.setId(e.getId());
        r.setInstanceId(e.getInstanceId());
        r.setLevel(e.getLevel());
        r.setMessage(e.getMessage());
        r.setCreateTime(e.getCreatedAt() != null ? e.getCreatedAt().format(DTF) : null);
        return r;
    }
}