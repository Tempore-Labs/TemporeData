package org.temporedata.api.svc.form;

import lombok.Data;

/**
 * Form submission response.
 */
@Data
public class FormSubmissionRes {

    private String id;

    private String formId;

    private String data;

    private String submitterIp;

    private String tenantId;

    private String submitTime;
}