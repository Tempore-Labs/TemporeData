package org.temporedata.api.sys.passwordless;

import lombok.Data;

/**
 * Passwordless config response DTO.
 */
@Data
public class PasswordlessConfigRes {

    private String id;

    private String loginType;

    private String config;

    private String status;

    private String tenantId;

    private String createTime;

    private String updateTime;
}