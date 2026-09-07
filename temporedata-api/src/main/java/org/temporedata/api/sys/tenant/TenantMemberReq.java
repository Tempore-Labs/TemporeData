package org.temporedata.api.sys.tenant;

import lombok.Data;

/**
 * Add tenant member request.
 */
@Data
public class TenantMemberReq {

    private String username;

    private String password;

    private String roleCode;

    private String orgId;
}