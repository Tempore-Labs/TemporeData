package org.temporedata.api.sys.agent;

import lombok.Data;

/**
 * Agent LLM config request.
 */
@Data
public class AgentConfigReq {

    private String provider;

    private String baseUrl;

    private String apiKey;

    private String model;

    private Double temperature;

    private Boolean enabled;
}