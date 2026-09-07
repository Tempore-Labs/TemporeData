package org.temporedata.api.dev.schedule;

import lombok.Data;

/**
 * Business calendar create / update request (P1-4).
 */
@Data
public class CalendarReq {

    private String name;
    private String description;
    /** CUSTOM | WEEK. */
    private String workdayMode;
    private String timezone;
    /** "1,2,3,4,5" (1=Mon..7=Sun) for WEEK mode. */
    private String weekWorkdays;
    private Integer cutHour;
    private Integer cutMinute;
}