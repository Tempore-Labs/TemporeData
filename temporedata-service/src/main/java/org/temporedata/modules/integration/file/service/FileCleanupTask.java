package org.temporedata.modules.integration.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled disk governance for the generic FileStorage module.
 * Reconcilies soft-deleted physical objects on a cron (default daily 03:00).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupTask {

    private final FileService fileService;

    @Scheduled(cron = "${app.file.clean.cron:0 0 3 * * *}")
    public void run() {
        try {
            int removed = fileService.cleanup();
            if (removed > 0) {
                log.info("Storage cleanup reconciled {} soft-deleted file object(s)", removed);
            }
        } catch (Exception e) {
            log.warn("Storage cleanup failed: {}", e.getMessage());
        }
    }
}