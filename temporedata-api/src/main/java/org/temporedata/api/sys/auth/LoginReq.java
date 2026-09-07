package org.temporedata.api.sys.auth;

import lombok.Data;

/**
 * Login request payload.
 */
@Data
public class LoginReq {

    private String username;

    private String password;
}