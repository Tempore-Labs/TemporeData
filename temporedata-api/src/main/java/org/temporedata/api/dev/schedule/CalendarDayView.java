package org.temporedata.api.dev.schedule;

import lombok.Data;

/**
 * One day in a calendar preview (P1-4).
 */
@Data
public class CalendarDayView {

    private String date;
    private boolean workday;
    private String dayType;
    private String remark;
}