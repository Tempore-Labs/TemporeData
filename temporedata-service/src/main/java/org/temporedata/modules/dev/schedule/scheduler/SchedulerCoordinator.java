package org.temporedata.modules.dev.schedule.scheduler;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.common.locker.DistributedLockProvider;
import org.temporedata.modules.dev.schedule.entity.TaskDefineEntity;
import org.temporedata.modules.dev.schedule.repository.TaskDefineRepository;
import org.temporedata.modules.dev.schedule.service.TaskScheduleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Scheduler coordinator: polls enabled task definitions and fires due instances.
 *
 * Design (P0-1): table-driven polling instead of a heavyweight scheduler.
 * - A distributed lock guards the scan so multiple nodes do not double-fire.
 * - For each task, cron-utils locates all fire times inside a look-back window
 *   that have no instance yet (idempotent catch-up after downtime).
 * - Dispatched inline to the {@link TaskDispatcher} (execution is decoupled).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "temporedata.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerCoordinator {

    private static final String SCAN_LOCK_KEY = "scheduler:scan";

    private final TaskDefineRepository taskDefineRepository;
    private final TaskScheduleService taskScheduleService;
    private final DistributedLockProvider distributedLock;

    @Value("${temporedata.scheduler.lookback-seconds:300}")
    private long lookbackSeconds;

    @Scheduled(fixedDelayString = "${temporedata.scheduler.scan-interval-ms:10000}")
    public void scanDueTasks() {
        if (!distributedLock.tryLock(SCAN_LOCK_KEY, 2000)) {
            log.debug("Scheduler scan lock not acquired, skip");
            return;
        }
        try {
            List<TaskDefineEntity> tasks = taskDefineRepository.findByEnabledTrueAndStatus("NORMAL");
            if (tasks.isEmpty()) return;

            Duration lookback = Duration.ofSeconds(lookbackSeconds);
            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            for (TaskDefineEntity task : tasks) {
                try {
                    processTask(task, now, lookback);
                } catch (Exception e) {
                    log.error("Failed to process task {}", task.getId(), e);
                }
            }
        } finally {
            distributedLock.unlock(SCAN_LOCK_KEY);
        }
    }

    private void processTask(TaskDefineEntity task, ZonedDateTime now, Duration lookback) {
        Cron cron;
        try {
            CronDefinition cd = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
            cron = new CronParser(cd).parse(task.getCronExpression());
        } catch (Exception e) {
            log.warn("Skipping task {} due to invalid cron {}", task.getId(), task.getCronExpression());
            return;
        }
        ExecutionTime et = ExecutionTime.forCron(cron);
        ZoneId zone = toZone(task.getTimezone());

        // Enumerate every fire in the look-back window and fire those with no
        // instance yet (idempotent catch-up after downtime).
        ZonedDateTime from = now.minus(lookback);
        int fired = 0;
        List<ZonedDateTime> fires = et.getExecutionDates(from, now);
        for (ZonedDateTime candidate : fires) {
            if (candidate.isAfter(now)) continue;
            LocalDateTime trigger = candidate.withZoneSameInstant(zone).toLocalDateTime();
            if (taskScheduleService.fire(task, trigger)) fired++;
        }
        if (fired > 0) {
            log.info("Fired {} instance(s) for task {} (cron {})", fired, task.getId(), task.getCronExpression());
        }
    }

    private ZoneId toZone(String timezone) {
        try {
            return timezone == null || timezone.isBlank() ? ZoneId.systemDefault() : ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.systemDefault();
        }
    }
}