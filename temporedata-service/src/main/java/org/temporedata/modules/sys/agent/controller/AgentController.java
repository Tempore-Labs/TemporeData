package org.temporedata.modules.sys.agent.controller;

import org.temporedata.modules.sys.agent.entity.AgentEntity;
import org.temporedata.modules.sys.agent.service.AgentService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/chat")
    public BaseResponse<AgentEntity> chat(@RequestBody AgentEntity entity) {
        return BaseResponse.success(agentService.chat(entity));
    }

    @GetMapping("/config")
    public BaseResponse<AgentEntity> getConfig() {
        return BaseResponse.success(agentService.getConfig());
    }

    @PostMapping("/config")
    public BaseResponse<AgentEntity> saveConfig(@RequestBody AgentEntity entity) {
        return BaseResponse.success(agentService.saveConfig(entity));
    }

    @PostMapping("/config/test")
    public BaseResponse<Map<String, Object>> testConfig(@RequestBody AgentEntity entity) {
        return BaseResponse.success(agentService.testConfig(entity));
    }

    @GetMapping("/sessions")
    public BaseResponse<List<Map<String, Object>>> getSessions() {
        return BaseResponse.success(agentService.getSessions());
    }

    @GetMapping("/history/{sessionId}")
    public BaseResponse<List<AgentEntity>> getHistory(@PathVariable String sessionId) {
        return BaseResponse.success(agentService.getHistory(sessionId));
    }

    @DeleteMapping("/session/{sessionId}")
    public BaseResponse<Void> deleteSession(@PathVariable String sessionId) {
        agentService.deleteSession(sessionId);
        return BaseResponse.success();
    }
}