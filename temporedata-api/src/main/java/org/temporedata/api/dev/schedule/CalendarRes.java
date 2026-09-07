package org.temporedata.api.dev.schedule;

import lombok.Data;

/**
 * Business calendar response (P1-4).
 */
@Data
public class CalendarRes {

    private String id;
    private String name;
    private String description;
    private String workdayMode;
    private String timezone;
    private String weekWorkdays;
    private Integer cutHour;
    private Integer cutMinute;
    private String createTime;
}