package org.temporedata.api.ops.alarm;

import lombok.Data;

/**
 * Alarm record response.
 */
@Data
public class AlarmRecordRes {

    private String id;

    private String eventType;

    private String message;

    private String status;

    private String sendTime;
}