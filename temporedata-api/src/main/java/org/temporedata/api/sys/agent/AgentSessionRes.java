package org.temporedata.api.sys.agent;

import lombok.Data;

/**
 * Agent chat session summary.
 */
@Data
public class AgentSessionRes {

    private String sessionId;

    private String title;

    private Integer messageCount;

    private String lastTime;
}