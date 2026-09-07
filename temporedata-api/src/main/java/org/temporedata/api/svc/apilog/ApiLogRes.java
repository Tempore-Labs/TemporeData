package org.temporedata.api.svc.apilog;

import lombok.Data;

/**
 * API log response DTO.
 */
@Data
public class ApiLogRes {

    private String id;

    private String apiName;

    private String apiPath;

    private String method;

    private String requestIp;

    private String requestParams;

    private Integer responseCode;

    private String responseBody;

    private Long costTime;

    private String callerId;

    private String tenantId;

    private String createTime;
}