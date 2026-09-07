package org.temporedata.api.dev.schedule;

import lombok.Data;

/**
 * Task definition response (P0-1 scheduler engine).
 */
@Data
public class TaskDefineRes {

    private String id;
    private String name;
    private String taskType;
    private String targetRef;
    private String cronExpression;
    private String bizDateMode;
    private String calendarId;
    private String timezone;
    private Boolean enabled;
    private String params;
    private String owner;
    private String status;
    private String createTime;
    private String updateTime;
}