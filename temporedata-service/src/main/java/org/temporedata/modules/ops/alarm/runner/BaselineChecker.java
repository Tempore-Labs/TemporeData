package org.temporedata.modules.ops.alarm.runner;

import lombok.extern.slf4j.Slf4j;
import org.temporedata.modules.ops.alarm.service.AlarmBaselineService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically runs the baseline evaluation loop (P2-8). Fires only if Spring
 * scheduling is enabled; the {@code /api/alarm/run-check} endpoint always works
 * for a manual run.
 */
@Slf4j
@Component
public class BaselineChecker {

    private final AlarmBaselineService alarmBaselineService;

    public BaselineChecker(AlarmBaselineService alarmBaselineService) {
        this.alarmBaselineService = alarmBaselineService;
    }

    @Scheduled(fixedDelay = 60000)
    public void check() {
        try {
            alarmBaselineService.checkBaselines();
        } catch (Exception e) {
            log.warn("Baseline check failed: {}", e.getMessage(), e);
        }
    }
}