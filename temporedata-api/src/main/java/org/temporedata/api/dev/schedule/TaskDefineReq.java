package org.temporedata.api.dev.schedule;

import lombok.Data;

/**
 * Task definition create / update request (P0-1 scheduler engine).
 */
@Data
public class TaskDefineReq {

    /** Task name. */
    private String name;

    /** Task type: WORKFLOW | SQL | HTTP | SHELL | QUALITY. */
    private String taskType;

    /** Target reference resolved by the dispatcher (workflow id, sql id, api id...). */
    private String targetRef;

    /** Cron expression. */
    private String cronExpression;

    /** Business date mode: NONE | DAY | WEEK | MONTH (P1). */
    private String bizDateMode;

    /** Linked calendar id (P1). */
    private String calendarId;

    /** Timezone, e.g. Asia/Shanghai. */
    private String timezone;

    /** Whether the task is enabled for scheduling. Boolean for API, persisted as TINYINT. */
    private Boolean enabled;

    /** Extra params injected on dispatch (JSON). */
    private String params;

    /** Owner / creator. */
    private String owner;
}