package org.temporedata.modules.ops.cluster.service;

import org.temporedata.modules.ops.cluster.entity.ClusterEntity;
import org.temporedata.modules.ops.cluster.repository.ClusterRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.temporedata.common.cache.CacheConfig.CACHE_CLUSTER;

@Slf4j @Service @RequiredArgsConstructor
public class ClusterService {

    private final ClusterRepository clusterRepository;

    /**
     * In-memory store for cluster nodes (simulated).
     */
    private final Map<String, List<Map<String, Object>>> clusterNodesStore = new HashMap<>();

    /**
     * List all clusters.
     */
    @Cacheable(value = CACHE_CLUSTER)
    public List<ClusterEntity> list() {
        return clusterRepository.findAll();
    }

    /**
     * Get cluster by id.
     */
    @Cacheable(value = CACHE_CLUSTER)
    public ClusterEntity get(String id) {
        return clusterRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cluster not found: " + id));
    }

    /**
     * Create a cluster.
     */
    @CacheEvict(value = CACHE_CLUSTER, allEntries = true)
    @Transactional
    public ClusterEntity create(ClusterEntity entity) {
        log.info("Creating cluster: name={}, type={}", entity.getName(), entity.getType());
        if (entity.getStatus() == null) {
            entity.setStatus("OFFLINE");
        }
        if (entity.getNodeCount() == null) {
            entity.setNodeCount(0);
        }
        ClusterEntity saved = clusterRepository.save(entity);
        clusterNodesStore.put(saved.getId(), new ArrayList<>());
        return saved;
    }

    /**
     * Update an existing cluster.
     */
    @CacheEvict(value = CACHE_CLUSTER, allEntries = true)
    @Transactional
    public ClusterEntity update(String id, ClusterEntity entity) {
        ClusterEntity existing = get(id);
        entity.setId(id);
        entity.setCreateDateTime(existing.getCreateDateTime());
        entity.setCreateBy(existing.getCreateBy());
        entity.setTenantId(existing.getTenantId());
        log.info("Updating cluster: id={}, name={}", id, entity.getName());
        return clusterRepository.save(entity);
    }

    /**
     * Delete a cluster by id.
     */
    @CacheEvict(value = CACHE_CLUSTER, allEntries = true)
    @Transactional
    public void delete(String id) {
        ClusterEntity entity = get(id);
        clusterRepository.delete(entity);
        clusterNodesStore.remove(id);
        log.info("Deleted cluster: id={}", id);
    }

    /**
     * Get nodes for a cluster (simulated).
     */
    @Cacheable(value = CACHE_CLUSTER)
    public List<Map<String, Object>> getNodes(String clusterId) {
        // Ensure cluster exists
        get(clusterId);
        return clusterNodesStore.getOrDefault(clusterId, new ArrayList<>());
    }

    /**
     * Add a node to a cluster (simulated).
     */
    @CacheEvict(value = CACHE_CLUSTER, allEntries = true)
    @Transactional
    public Map<String, Object> addNode(String clusterId, Map<String, Object> nodeReq) {
        ClusterEntity cluster = get(clusterId);
        log.info("Adding node to cluster: clusterId={}", clusterId);

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", UUID.randomUUID().toString().replace("-", ""));
        node.put("clusterId", clusterId);
        node.put("host", nodeReq.getOrDefault("host", "unknown"));
        node.put("port", nodeReq.getOrDefault("port", 0));
        node.put("status", "PENDING");
        node.put("createTime", now);

        clusterNodesStore.computeIfAbsent(clusterId, k -> new ArrayList<>()).add(node);

        // Update cluster node count
        cluster.setNodeCount(clusterNodesStore.get(clusterId).size());
        clusterRepository.save(cluster);

        log.info("Node added to cluster: clusterId={}, nodeId={}", clusterId, node.get("id"));
        return node;
    }

    /**
     * Install agent on a node (simulated).
     */
    @CacheEvict(value = CACHE_CLUSTER, allEntries = true)
    @Transactional
    public Map<String, Object> installAgent(String nodeId) {
        log.info("Installing agent on node: nodeId={}", nodeId);

        // Find the node in any cluster
        for (Map.Entry<String, List<Map<String, Object>>> entry : clusterNodesStore.entrySet()) {
            for (Map<String, Object> node : entry.getValue()) {
                if (nodeId.equals(node.get("id"))) {
                    String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    node.put("agentStatus", "INSTALLED");
                    node.put("installTime", now);

                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("nodeId", nodeId);
                    result.put("agentStatus", "INSTALLED");
                    result.put("message", "Agent installed successfully");
                    result.put("installTime", now);
                    log.info("Agent installed on node: nodeId={}", nodeId);
                    return result;
                }
            }
        }
        throw new BusinessException("Node not found: " + nodeId);
    }

    /**
     * Check node status (simulated).
     */
    @Cacheable(value = CACHE_CLUSTER)
    public Map<String, Object> checkNodeStatus(String nodeId) {
        log.info("Checking node status: nodeId={}", nodeId);

        // Find the node in any cluster
        for (Map.Entry<String, List<Map<String, Object>>> entry : clusterNodesStore.entrySet()) {
            for (Map<String, Object> node : entry.getValue()) {
                if (nodeId.equals(node.get("id"))) {
                    Map<String, Object> status = new LinkedHashMap<>();
                    status.put("nodeId", nodeId);
                    status.put("status", node.getOrDefault("status", "UNKNOWN"));
                    status.put("agentStatus", node.getOrDefault("agentStatus", "NOT_INSTALLED"));
                    status.put("cpuUsage", Math.random() * 100);
                    status.put("memoryUsage", Math.random() * 100);
                    status.put("diskUsage", Math.random() * 100);
                    status.put("uptime", String.format("%.1f hours", Math.random() * 720));
                    return status;
                }
            }
        }
        throw new BusinessException("Node not found: " + nodeId);
    }
}