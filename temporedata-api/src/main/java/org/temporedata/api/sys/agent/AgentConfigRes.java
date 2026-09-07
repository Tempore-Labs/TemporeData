package org.temporedata.api.sys.agent;

import lombok.Data;

/**
 * Agent LLM config response (apiKey masked).
 */
@Data
public class AgentConfigRes {

    private String provider;

    private String baseUrl;

    private String model;

    private Double temperature;

    private Boolean enabled;

    private String apiKeyMasked;

    private Boolean apiKeySet;
}