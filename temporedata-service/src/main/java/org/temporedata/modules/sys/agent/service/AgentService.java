package org.temporedata.modules.sys.agent.service;

import org.temporedata.modules.sys.agent.entity.AgentEntity;
import org.temporedata.modules.sys.agent.repository.AgentRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.temporedata.common.cache.CacheConfig.CACHE_AGENT;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;

    /**
     * Simulated AI chat reply.
     */
    @CacheEvict(value = CACHE_AGENT, allEntries = true)
    @Transactional
    public AgentEntity chat(AgentEntity request) {
        log.info("Agent chat: sessionId={}, message={}", request.getSessionId(), request.getMessage());

        // Generate a session ID if not provided
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }

        // Build the user message entity
        AgentEntity userMsg = AgentEntity.builder()
                .sessionId(sessionId)
                .message(request.getMessage())
                .context(request.getContext())
                .modelName(request.getModelName())
                .action("user")
                .build();
        agentRepository.save(userMsg);

        // Simulate AI reply
        String reply = generateReply(request.getMessage(), request.getContext());
        AgentEntity aiMsg = AgentEntity.builder()
                .sessionId(sessionId)
                .message(request.getMessage())
                .reply(reply)
                .action("assistant")
                .modelName(request.getModelName() != null ? request.getModelName() : "default")
                .ok(true)
                .build();
        AgentEntity saved = agentRepository.save(aiMsg);

        log.info("Agent reply generated: sessionId={}", sessionId);
        return saved;
    }

    /**
     * Get the current LLM config (the first enabled config or a default).
     */
    @Cacheable(value = CACHE_AGENT)
    public AgentEntity getConfig() {
        List<AgentEntity> configs = agentRepository.findAll().stream()
                .filter(a -> a.getLlm() != null && a.getLlm())
                .collect(Collectors.toList());
        if (configs.isEmpty()) {
            // Return a default config
            return AgentEntity.builder()
                    .provider("openai")
                    .baseUrl("https://api.openai.com/v1")
                    .model("gpt-4")
                    .temperature(0.7)
                    .enabled(true)
                    .apiKeySet(false)
                    .build();
        }
        AgentEntity config = configs.get(0);
        // Mask the API key for security
        config.setApiKey(null);
        return config;
    }

    /**
     * Save LLM config.
     */
    @CacheEvict(value = CACHE_AGENT, allEntries = true)
    @Transactional
    public AgentEntity saveConfig(AgentEntity config) {
        log.info("Saving agent config: provider={}, model={}", config.getProvider(), config.getModel());

        // Disable existing LLM configs
        List<AgentEntity> existing = agentRepository.findAll().stream()
                .filter(a -> a.getLlm() != null && a.getLlm())
                .collect(Collectors.toList());
        for (AgentEntity e : existing) {
            e.setLlm(false);
            agentRepository.save(e);
        }

        // Create new config entity
        AgentEntity entity = AgentEntity.builder()
                .llm(true)
                .provider(config.getProvider())
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .model(config.getModel())
                .temperature(config.getTemperature() != null ? config.getTemperature() : 0.7)
                .enabled(true)
                .apiKeySet(config.getApiKey() != null && !config.getApiKey().isEmpty())
                .build();
        if (entity.getApiKeySet()) {
            entity.setApiKeyMasked(maskApiKey(config.getApiKey()));
        }
        return agentRepository.save(entity);
    }

    /**
     * Test the LLM config by simulating a connection.
     */
    public Map<String, Object> testConfig(AgentEntity config) {
        log.info("Testing agent config: provider={}, model={}", config.getProvider(), config.getModel());
        // Simulate a test call
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("message", "Connection successful");
        result.put("provider", config.getProvider());
        result.put("model", config.getModel());
        return result;
    }

    /**
     * Get all sessions (distinct session IDs with latest message info).
     */
    @Cacheable(value = CACHE_AGENT)
    public List<Map<String, Object>> getSessions() {
        List<AgentEntity> all = agentRepository.findAll();
        Map<String, List<AgentEntity>> grouped = all.stream()
                .filter(a -> a.getSessionId() != null)
                .collect(Collectors.groupingBy(AgentEntity::getSessionId));

        List<Map<String, Object>> sessions = new ArrayList<>();
        for (Map.Entry<String, List<AgentEntity>> entry : grouped.entrySet()) {
            List<AgentEntity> msgs = entry.getValue();
            AgentEntity last = msgs.get(msgs.size() - 1);
            Map<String, Object> session = new LinkedHashMap<>();
            session.put("sessionId", entry.getKey());
            session.put("title", last.getTitle() != null ? last.getTitle() : "New Chat");
            session.put("messageCount", msgs.size());
            session.put("lastTime", last.getCreateDateTime() != null
                    ? last.getCreateDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : null);
            sessions.add(session);
        }
        // Sort by lastTime descending
        sessions.sort((a, b) -> {
            String ta = (String) a.get("lastTime");
            String tb = (String) b.get("lastTime");
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return sessions;
    }

    /**
     * Get chat history for a session.
     */
    @Cacheable(value = CACHE_AGENT)
    public List<AgentEntity> getHistory(String sessionId) {
        return agentRepository.findBySessionIdOrderByCreateDateTimeAsc(sessionId);
    }

    /**
     * Delete a session and all its messages.
     */
    @CacheEvict(value = CACHE_AGENT, allEntries = true)
    @Transactional
    public void deleteSession(String sessionId) {
        log.info("Deleting agent session: {}", sessionId);
        agentRepository.deleteBySessionId(sessionId);
    }

    /**
     * Generate a simulated AI reply. When a data-object context is attached
     * (v2.0 §24), the reply follows the "conclusion + evidence + recommended action"
     * pattern expected of a context-aware copilot.
     */
    private String generateReply(String message, String context) {
        if (message == null || message.isEmpty()) {
            return "你好，我是轻舟智助。请输入你的问题，例如：找出今天失败的数据任务并分析原因。";
        }
        String lower = message.toLowerCase();

        // Context-aware copilot reply over the object the user is currently viewing
        if (context != null && !context.isBlank()) {
            String obj = context.trim();
            return "【结论】针对数据对象 " + obj + " 的分析如下。\n"
                    + "【证据】已基于该对象的元数据、运行记录与质量/权限信息完成排查，未发现阻断性异常；若存在告警，可通过下方操作直达告警中心查看明细。\n"
                    + "【推荐操作】\n"
                    + "  1. 查看字段与血缘 → 定位 " + obj + " 的上游来源与下游依赖\n"
                    + "  2. 查看质量检测 → 确认最新质量得分与失败规则\n"
                    + "  3. 查看权限与调用 → 核对访问范围与服务调用情况\n"
                    + "你可以进一步描述意图（例如：为什么失败 / 有哪些敏感字段 / 生成每日任务），我会给出更具体的结果。";
        }

        if (lower.contains("hello") || lower.contains("hi") || lower.contains("你好")) {
            return "你好，我是轻舟智助。我可以帮你检索数据资产、分析任务失败、检查数据质量与血缘，请描述你的需求。";
        } else if (lower.contains("help") || lower.contains("帮助") || lower.contains("能做什么")) {
            return "我可以为你提供：\n"
                    + "  1) 找数据：搜索表/字段/指标/标签\n"
                    + "  2) 排查失败：分析任务失败原因与血缘\n"
                    + "  3) 治理：敏感字段、分类分级、质量稽核\n"
                    + "  4) 创建任务：生成每日 8 点执行的同步/调度任务\n"
                    + "请描述具体问题，我按“结论 + 证据 + 推荐操作”给出答复。";
        } else if (lower.contains("失败") || lower.contains("failed")) {
            return "【结论】发现以下隐患需要关注。\n【证据】任务延迟或失败常见为上游字段变更、资源超限或依赖缺失。\n【推荐操作】前往「运行中心·调度任务」查看最近运行，或用「总览」的异常事项进入任务详情，再通过血缘定位上游影响。";
        } else if (lower.contains("敏感") || lower.contains("sensitive")) {
            return "【结论】该数据对象存在敏感性风险，需按分类分级落实脱敏。\n【证据】字段级别安全标记可被审计与分类分级识别。\n【推荐操作】前往「安全治理·分类分级」核对级别，并为敏感字段配置脱敏策略。";
        } else if (lower.contains("quality") || lower.contains("质量")) {
            return "【结论】我建议检查该表的数据质量得分与最近检测结果。\n【证据】质量规则、失败记录与异常数据量可在质量中心查看。\n【推荐操作】前往「数据资产·数据质量」查看质量报告并处理失败规则。";
        } else if (lower.contains("每日") || lower.contains("每天") || lower.contains("每天 8") || lower.contains("schedule")) {
            return "【结论】可以为你创建每天 8 点执行的调度任务。\n【推荐操作】前往「数据开发·工作流」新建任务并配置调度周期；或在「运行中心·调度任务」设置每日 8:00 触发。";
        } else {
            return "我已理解你的问题并开始分析。基于可用信息，建议先定位相关数据对象或任务：可前往「总览」查看运行态势与待办事项，或用 Ctrl+K 全局搜索直达目标对象。如需更具体结论，请补充对象名称或场景。";
        }
    }

    /**
     * Mask an API key for display.
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}