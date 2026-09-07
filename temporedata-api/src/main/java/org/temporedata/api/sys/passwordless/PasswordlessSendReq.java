package org.temporedata.api.sys.passwordless;

import lombok.Data;

/**
 * Passwordless send code request DTO.
 */
@Data
public class PasswordlessSendReq {

    private String loginType;

    private String target;
}