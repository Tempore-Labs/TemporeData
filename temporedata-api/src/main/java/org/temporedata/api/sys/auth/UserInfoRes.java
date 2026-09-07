package org.temporedata.api.sys.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Current logged-in user info returned to the frontend.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoRes {

    private String userId;

    private String username;

    private String tenantId;

    private String tenantName;

    private List<String> roles;

    private List<String> permissions;

    /** Whether the current user holds super-admin (ROLE_ADMIN / bypass). */
    private boolean isAdmin;

    /** Personal center profile fields. */
    private String nickname;

    private String phone;

    private String email;
}