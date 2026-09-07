package org.temporedata.api.sys.passwordless;

import lombok.Data;

/**
 * Passwordless config request DTO.
 */
@Data
public class PasswordlessConfigReq {

    private String loginType;

    private String config;
}