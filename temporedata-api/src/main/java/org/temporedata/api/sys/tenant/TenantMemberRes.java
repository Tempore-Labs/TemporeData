package org.temporedata.api.sys.tenant;

import lombok.Data;

import java.util.Set;

/**
 * Tenant member response.
 */
@Data
public class TenantMemberRes {

    private String id;

    private String username;

    private Set<String> roles;

    private Integer status;

    private String orgId;

    private String orgName;

    private String createTime;
}