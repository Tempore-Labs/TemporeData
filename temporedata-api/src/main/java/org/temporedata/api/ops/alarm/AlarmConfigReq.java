package org.temporedata.api.ops.alarm;

import lombok.Data;

/**
 * Create / update alarm config request.
 */
@Data
public class AlarmConfigReq {

    private String name;

    private String eventType; // WORKFLOW_FAILED, QUALITY_FAILED, NODE_CRASH

    private String channels; // comma-separated: EMAIL,SMS,WEBHOOK

    private String email;

    private String webhookUrl;

    private Boolean enabled;
}