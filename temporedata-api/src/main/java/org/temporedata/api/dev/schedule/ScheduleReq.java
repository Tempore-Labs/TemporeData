package org.temporedata.api.dev.schedule;

import lombok.Data;

/**
 * Schedule create / update request.
 */
@Data
public class ScheduleReq {

    private String cronExpression;

    private Integer enabled;
}