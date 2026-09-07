package org.temporedata.api.sys.agent;

import lombok.Data;

/**
 * Agent LLM connection test result.
 */
@Data
public class AgentTestRes {

    private Boolean ok;

    private String message;
}