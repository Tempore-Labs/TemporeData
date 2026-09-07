package org.temporedata.api.dev.schedule;

import lombok.Data;

/**
 * One derived business date in a preview / backfill (P1-4).
 */
@Data
public class BizDatePreviewItem {

    private String bizDate;
    private boolean workday;
    /** Would-be trigger time (a suggestion; precise time comes from cron). */
    private String triggerTime;
}