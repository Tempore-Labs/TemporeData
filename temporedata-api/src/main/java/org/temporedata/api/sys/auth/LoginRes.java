package org.temporedata.api.sys.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login response payload: JWT token + basic user info.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRes {

    private String token;

    private UserInfoRes user;
}