package org.temporedata.api.sys.tenant;

import lombok.Data;

/**
 * Tenant list / detail response.
 */
@Data
public class TenantRes {

    private String id;

    private String name;

    private String code;

    private String description;

    private String contactName;

    private String contactEmail;

    private String status;

    private Integer memberCount;

    private String createTime;
}