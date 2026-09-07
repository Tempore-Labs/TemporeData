package org.temporedata.modules.dev.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.dev.workflow.*;
import org.temporedata.modules.dev.workflow.entity.WorkflowEdgeEntity;
import org.temporedata.modules.dev.workflow.entity.WorkflowEntity;
import org.temporedata.modules.dev.workflow.entity.WorkflowNodeEntity;
import org.temporedata.modules.dev.workflow.repository.WorkflowRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.temporedata.common.cache.CacheConfig.CACHE_WORKFLOW;

@Slf4j @Service @RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final ObjectMapper objectMapper;

    // ---- CRUD ----

    @Cacheable(value = CACHE_WORKFLOW)
    public List<WorkflowRes> list() {
        return workflowRepository.findAll().stream()
                .map(this::toWorkflowRes)
                .collect(Collectors.toList());
    }

    @Cacheable(value = CACHE_WORKFLOW)
    public WorkflowRes get(String id) {
        return toWorkflowRes(findEntity(id));
    }

    @Transactional
    @CacheEvict(value = CACHE_WORKFLOW, allEntries = true)
    public WorkflowRes create(WorkflowReq req) {
        String tenantId = org.temporedata.security.context.TenantContext.getTenantId();
        WorkflowEntity entity = WorkflowEntity.builder()
                .name(req.getName())
                .description(req.getDescription())
                .status("DRAFT")
                .scheduleEnabled(false)
                .tenantId(tenantId == null ? "DEFAULT" : tenantId)
                .nodesJson(toJson(req.getNodes()))
                .edgesJson(toJson(req.getEdges()))
                .build();
        return toWorkflowRes(workflowRepository.save(entity));
    }

    @Transactional
    @CacheEvict(value = CACHE_WORKFLOW, allEntries = true)
    public WorkflowRes update(String id, WorkflowReq req) {
        WorkflowEntity entity = findEntity(id);
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setNodesJson(toJson(req.getNodes()));
        entity.setEdgesJson(toJson(req.getEdges()));
        return toWorkflowRes(workflowRepository.save(entity));
    }

    @Transactional
    @CacheEvict(value = CACHE_WORKFLOW, allEntries = true)
    public void delete(String id) {
        workflowRepository.deleteById(id);
    }

    // ---- Execute ----

    @Transactional
    @CacheEvict(value = CACHE_WORKFLOW, allEntries = true)
    public WorkflowExecuteRes execute(String id) {
        WorkflowEntity entity = findEntity(id);
        String executeId = UUID.randomUUID().toString().replace("-", "");
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<WorkflowExecuteRes.NodeExecuteItem> nodeResults = parseNodes(entity.getNodesJson()).stream()
                .map(n -> {
                    WorkflowExecuteRes.NodeExecuteItem item = new WorkflowExecuteRes.NodeExecuteItem();
                    item.setNodeId(n.getId());
                    item.setNodeName(n.getName());
                    item.setStatus("PENDING");
                    return item;
                })
                .collect(Collectors.toList());

        WorkflowExecuteRes result = new WorkflowExecuteRes();
        result.setExecuteId(executeId);
        result.setStatus("RUNNING");
        result.setNodeResults(nodeResults);
        result.setStartTime(now);

        // Store execution record
        List<WorkflowExecuteRes> executions = parseExecutions(entity.getExecutionsJson());
        executions.add(result);
        entity.setExecutionsJson(toJson(executions));
        entity.setLastExecuteTime(LocalDateTime.now());
        entity.setStatus("RUNNING");
        workflowRepository.save(entity);

        return result;
    }

    // ---- Schedule ----

    @Transactional
    @CacheEvict(value = CACHE_WORKFLOW, allEntries = true)
    public WorkflowRes schedule(String id, String cronExpression, Boolean enabled) {
        WorkflowEntity entity = findEntity(id);
        entity.setScheduleCron(cronExpression);
        if (enabled != null) {
            entity.setScheduleEnabled(enabled);
        }
        return toWorkflowRes(workflowRepository.save(entity));
    }

    // ---- Executions ----

    @Cacheable(value = CACHE_WORKFLOW)
    public List<WorkflowExecuteRes> executions(String id) {
        WorkflowEntity entity = findEntity(id);
        return parseExecutions(entity.getExecutionsJson());
    }

    // ---- Internal helpers ----

    private WorkflowEntity findEntity(String id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Workflow not found: " + id));
    }

    private WorkflowRes toWorkflowRes(WorkflowEntity entity) {
        WorkflowRes res = new WorkflowRes();
        res.setId(entity.getId());
        res.setName(entity.getName());
        res.setDescription(entity.getDescription());
        res.setStatus(entity.getStatus());
        res.setCronExpression(entity.getScheduleCron());
        res.setCreateTime(entity.getCreateDateTime() != null
                ? entity.getCreateDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : null);
        res.setNodes(parseNodeRes(entity.getNodesJson()));
        res.setEdges(parseEdgeRes(entity.getEdgesJson()));
        return res;
    }

    // ---- JSON conversion helpers ----

    private List<WorkflowNodeRes> parseNodeRes(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<WorkflowNodeEntity> nodes = objectMapper.readValue(json, new TypeReference<List<WorkflowNodeEntity>>() {});
            return nodes.stream().map(this::toNodeRes).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to parse nodesJson", e);
            return Collections.emptyList();
        }
    }

    private List<WorkflowEdgeRes> parseEdgeRes(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<WorkflowEdgeEntity> edges = objectMapper.readValue(json, new TypeReference<List<WorkflowEdgeEntity>>() {});
            return edges.stream().map(this::toEdgeRes).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to parse edgesJson", e);
            return Collections.emptyList();
        }
    }

    private List<WorkflowNodeEntity> parseNodes(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<WorkflowNodeEntity>>() {});
        } catch (Exception e) {
            log.error("Failed to parse nodesJson", e);
            return Collections.emptyList();
        }
    }

    private List<WorkflowExecuteRes> parseExecutions(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<WorkflowExecuteRes>>() {});
        } catch (Exception e) {
            log.error("Failed to parse executionsJson", e);
            return new ArrayList<>();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new BusinessException("JSON serialization error: " + e.getMessage());
        }
    }

    private WorkflowNodeRes toNodeRes(WorkflowNodeEntity n) {
        WorkflowNodeRes r = new WorkflowNodeRes();
        r.setId(n.getId());
        r.setName(n.getName());
        r.setType(n.getType());
        r.setDatasourceId(n.getDatasourceId());
        r.setDatasourceName(n.getDatasourceName());
        r.setSql(n.getSql());
        r.setPositionX(n.getPositionX());
        r.setPositionY(n.getPositionY());
        r.setRetryCount(n.getRetryCount());
        r.setRetryInterval(n.getRetryInterval());
        r.setTimeoutSeconds(n.getTimeoutSeconds());
        r.setPriority(n.getPriority());
        r.setFailStrategy(n.getFailStrategy());
        r.setParams(n.getParams());
        r.setSparkConf(n.getSparkConf());
        r.setHttpUrl(n.getHttpUrl());
        r.setHttpMethod(n.getHttpMethod());
        r.setHttpHeaders(n.getHttpHeaders());
        r.setDependencyType(n.getDependencyType());
        r.setDependencyTimeout(n.getDependencyTimeout());
        r.setEmailTo(n.getEmailTo());
        r.setEmailSubject(n.getEmailSubject());
        r.setQualityType(n.getQualityType());
        r.setQualityThreshold(n.getQualityThreshold());
        return r;
    }

    private WorkflowEdgeRes toEdgeRes(WorkflowEdgeEntity e) {
        WorkflowEdgeRes r = new WorkflowEdgeRes();
        r.setId(e.getId());
        r.setSourceNodeId(e.getSourceNodeId());
        r.setTargetNodeId(e.getTargetNodeId());
        r.setEdgeType(e.getEdgeType());
        r.setConditionExpr(e.getConditionExpr());
        return r;
    }
}