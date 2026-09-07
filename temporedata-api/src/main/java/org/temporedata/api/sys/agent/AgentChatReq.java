package org.temporedata.api.sys.agent;

import lombok.Data;

/**
 * Agent chat request.
 */
@Data
public class AgentChatReq {

    private String message;

    private String context; // optional: conversation context

    private String sessionId; // optional: conversation session

    private String modelName; // optional: model override
}