package org.temporedata.api.dev.schedule;

import lombok.Data;

/**
 * One day override when editing a calendar (P1-4).
 */
@Data
public class CalendarDaySaveItem {

    private String date;
    private Boolean workday;
    private String remark;
}