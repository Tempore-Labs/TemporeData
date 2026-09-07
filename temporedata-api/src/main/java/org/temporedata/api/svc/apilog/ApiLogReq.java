package org.temporedata.api.svc.apilog;

import lombok.Data;

/**
 * API log request DTO.
 */
@Data
public class ApiLogReq {

    private String apiName;

    private String apiPath;

    private String method;

    private String requestIp;

    private String requestParams;

    private Integer responseCode;

    private String responseBody;

    private Long costTime;

    private String callerId;
}