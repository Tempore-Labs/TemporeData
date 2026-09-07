package org.temporedata.api.sys.agent;

import lombok.Data;

import java.util.List;

/**
 * Agent chat response.
 */
@Data
public class AgentChatRes {

    private String reply;

    private String action; // SUGGEST_SQL, SUGGEST_WORKFLOW, GENERAL

    private List<String> suggestions; // suggested next actions

    private String sessionId; // conversation session id

    private Boolean llm; // whether reply came from LLM
}