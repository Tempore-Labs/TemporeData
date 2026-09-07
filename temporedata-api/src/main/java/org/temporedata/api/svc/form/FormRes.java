package org.temporedata.api.svc.form;

import lombok.Data;

/**
 * Form list / detail response.
 */
@Data
public class FormRes {

    private String id;

    private String name;

    private String description;

    private String config;

    private String status;

    private String shareToken;

    private String tenantId;

    private String createTime;

    private String updateTime;
}