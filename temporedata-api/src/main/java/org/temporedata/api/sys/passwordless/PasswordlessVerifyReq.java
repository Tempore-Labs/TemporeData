package org.temporedata.api.sys.passwordless;

import lombok.Data;

/**
 * Passwordless verify code request DTO.
 */
@Data
public class PasswordlessVerifyReq {

    private String loginType;

    private String target;

    private String code;
}