package org.temporedata.api.sys.role;

import lombok.Data;

/**
 * Role member response - shows a user who holds a role.
 */
@Data
public class RoleMemberRes {

    private String id;

    private String username;

    private String tenantId;

    private String tenantName;

    private String createTime;
}
