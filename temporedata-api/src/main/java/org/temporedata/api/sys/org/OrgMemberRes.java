package org.temporedata.api.sys.org;

import lombok.Data;

import java.util.Set;

/**
 * Department member response - shows the user assigned to a department.
 */
@Data
public class OrgMemberRes {

    private String id;

    private String username;

    private String nickname;

    private Set<String> roles;

    private String tenantId;

    private String createTime;
}
