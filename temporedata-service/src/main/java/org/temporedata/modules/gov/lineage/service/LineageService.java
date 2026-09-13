package org.temporedata.modules.gov.lineage.service;

import org.temporedata.modules.dev.workflow.entity.WorkflowLineageEntity;
import org.temporedata.modules.dev.workflow.repository.WorkflowLineageRepository;
import org.temporedata.modules.gov.lineage.entity.LineageEntity;
import org.temporedata.modules.gov.lineage.repository.LineageRepository;
import org.temporedata.modules.gov.meta.entity.MetaTableEntity;
import org.temporedata.modules.gov.meta.repository.MetaTableRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.gov.lineage.LineageDetails;
import org.temporedata.api.gov.lineage.LineageEdge;
import org.temporedata.api.gov.lineage.Source;
import org.temporedata.modules.gov.lineage.security.LineageAuthHelper;
import org.temporedata.modules.gov.lineage.support.LineageGraphCache;
import org.temporedata.modules.gov.lineage.support.LineageMapper;
import org.temporedata.modules.gov.lineage.support.LineageWriteGate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import static org.temporedata.common.cache.CacheConfig.CACHE_LINEAGE;

@Slf4j
@Service
@RequiredArgsConstructor
public class LineageService {

    private final LineageRepository lineageRepository;
    private final WorkflowLineageRepository workflowLineageRepository;
    private final MetaTableRepository metaTableRepository;
    private final ObjectMapper objectMapper;
    private final LineageWriteGate writeGate;
    private final LineageGraphCache graphCache;
    private final LineageAuthHelper authHelper;
    private final LineageMapper lineageMapper;

    /**
     * Rebuild the lineage graph from the REAL task-level workflow lineage
     * ({@code zy_workflow_lineage}) plus standalone nodes from the collected
     * metadata tables ({@code zy_meta_table}). Task nodes mediate READ/WRITE edges
     * between source and target tables.
     */
    @CacheEvict(value = CACHE_LINEAGE, allEntries = true)
    @Transactional
    public List<LineageEntity> rebuild() {
        // Clear existing lineage data
        lineageRepository.deleteAll();

        List<LineageEntity> lineageList = new ArrayList<>();
        Set<String> nodeIds = new LinkedHashSet<>();

        for (WorkflowLineageEntity r : workflowLineageRepository.findAll()) {
            String task = "task:" + r.getNodeId();
            String taskName = r.getNodeName() != null ? r.getNodeName() : task;
            if (r.getSourceTable() != null) {
                LineageEntity le = LineageEntity.builder()
                        .id(UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                        .name(r.getSourceTable() + " -> " + taskName)
                        .nodeType("TABLE")
                        .sourceId("tbl:" + r.getSourceTable())
                        .targetId(task)
                        .sourceName(r.getSourceTable())
                        .targetName(taskName)
                        .edgeType("READ")
                        .hasCycle(false)
                        .build();
                lineageList.add(le);
                nodeIds.add(le.getSourceId());
                nodeIds.add(le.getTargetId());
            }
            if (r.getTargetTable() != null) {
                LineageEntity le = LineageEntity.builder()
                        .id(UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                        .name(taskName + " -> " + r.getTargetTable())
                        .nodeType("TABLE")
                        .sourceId(task)
                        .targetId("tbl:" + r.getTargetTable())
                        .sourceName(taskName)
                        .targetName(r.getTargetTable())
                        .edgeType("WRITE")
                        .hasCycle(false)
                        .build();
                lineageList.add(le);
                nodeIds.add(le.getSourceId());
                nodeIds.add(le.getTargetId());
            }
        }

        // Standalone nodes from the real collected metadata tables
        for (MetaTableEntity t : metaTableRepository.findAll()) {
            String id = "tbl:" + t.getTableName();
            if (nodeIds.contains(id)) {
                continue;
            }
            LineageEntity node = LineageEntity.builder()
                    .id(UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                    .name(t.getTableName())
                    .nodeType("TABLE")
                    .sourceId(id)
                    .inDegree(t.getLineageCount() != null ? t.getLineageCount() : 0)
                    .build();
            lineageList.add(node);
            nodeIds.add(id);
        }

        // Calculate degree info
        for (LineageEntity le : lineageList) {
            if (le.getSourceId() != null && le.getTargetId() != null) {
                le.setInDegree((int) lineageList.stream().filter(l -> le.getSourceId() != null && le.getSourceId().equals(l.getTargetId())).count());
                le.setOutDegree((int) lineageList.stream().filter(l -> le.getSourceId() != null && le.getSourceId().equals(l.getSourceId())).count());
            }
        }

        List<LineageEntity> saved = lineageRepository.saveAll(lineageList);
        log.info("Rebuilt lineage graph with {} nodes/edges from real workflow lineage", saved.size());
        return saved;
    }

    /**
     * Get lineage graph data with filtering.
     */
    @Cacheable(value = CACHE_LINEAGE, unless = "#result == null || #result.isEmpty()")
    public Map<String, Object> getGraph(String nodeType, String keyword, Integer maxDepth, String nodeId) {
        List<LineageEntity> all = lineageRepository.findAll();

        // Filter by node type
        List<LineageEntity> filtered = all;
        if (nodeType != null && !nodeType.isEmpty()) {
            filtered = filtered.stream()
                    .filter(e -> nodeType.equals(e.getNodeType()))
                    .collect(Collectors.toList());
        }

        // Filter by keyword
        if (keyword != null && !keyword.isEmpty()) {
            String kw = keyword.toLowerCase();
            filtered = filtered.stream()
                    .filter(e -> (e.getName() != null && e.getName().toLowerCase().contains(kw))
                            || (e.getSourceName() != null && e.getSourceName().toLowerCase().contains(kw))
                            || (e.getTargetName() != null && e.getTargetName().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }

        // Anchor on a root node when provided (from the search box): restrict the graph
        // to the reachable component within maxDepth so "分析血缘" focuses a chosen table.
        String rootId = null;
        if (nodeId != null && !nodeId.isEmpty()) {
            rootId = nodeId;
            int depth = maxDepth != null && maxDepth > 0 ? maxDepth : 3;
            filtered = reachable(filtered, rootId, depth);
        }

        // Build nodes and edges for graph
        Set<String> nodeIds = new LinkedHashSet<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        for (LineageEntity le : filtered) {
            if (le.getSourceId() != null && le.getTargetId() != null) {
                // This is an edge
                if (nodeIds.add(le.getSourceId())) {
                    Map<String, Object> n = new LinkedHashMap<>();
                    n.put("id", le.getSourceId());
                    n.put("name", le.getSourceName());
                    n.put("nodeType", le.getNodeType());
                    n.put("inDegree", le.getInDegree());
                    n.put("outDegree", le.getOutDegree());
                    nodes.add(n);
                }
                if (nodeIds.add(le.getTargetId())) {
                    Map<String, Object> n = new LinkedHashMap<>();
                    n.put("id", le.getTargetId());
                    n.put("name", le.getTargetName());
                    n.put("nodeType", le.getNodeType());
                    n.put("inDegree", le.getInDegree());
                    n.put("outDegree", le.getOutDegree());
                    nodes.add(n);
                }
                Map<String, Object> edge = new LinkedHashMap<>();
                edge.put("source", le.getSourceId());
                edge.put("target", le.getTargetId());
                edge.put("label", le.getEdgeType());
                edges.add(edge);
            } else if (le.getSourceId() != null && !nodeIds.contains(le.getSourceId())) {
                // Standalone node
                nodeIds.add(le.getSourceId());
                Map<String, Object> n = new LinkedHashMap<>();
                n.put("id", le.getSourceId());
                n.put("name", le.getName());
                n.put("nodeType", le.getNodeType());
                n.put("inDegree", le.getInDegree());
                n.put("outDegree", le.getOutDegree());
                nodes.add(n);
            }
        }

        // Apply maxDepth pruning if set
        if (maxDepth != null && maxDepth > 0 && !nodes.isEmpty()) {
            // Simple BFS-based depth pruning
            Map<String, Integer> depthMap = new HashMap<>();
            String bfsRoot = nodes.get(0).get("id").toString();
            Queue<String> queue = new LinkedList<>();
            queue.add(bfsRoot);
            depthMap.put(bfsRoot, 0);
            while (!queue.isEmpty()) {
                String current = queue.poll();
                int depth = depthMap.get(current);
                if (depth >= maxDepth) continue;
                for (Map<String, Object> edge : edges) {
                    String target = (String) edge.get("target");
                    if (current.equals(edge.get("source")) && !depthMap.containsKey(target)) {
                        depthMap.put(target, depth + 1);
                        queue.add(target);
                    }
                }
            }
            // Prune nodes beyond maxDepth
            Set<String> validIds = depthMap.keySet();
            nodes.removeIf(n -> !validIds.contains(n.get("id")));
            edges.removeIf(e -> !validIds.contains(e.get("source")) || !validIds.contains(e.get("target")));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        String rootName = rootId != null ? resolveNodeName(rootId) : null;
        if (rootName == null && !nodes.isEmpty()) {
            rootName = String.valueOf(nodes.get(0).get("name"));
        }
        final String rootKey = rootId;
        result.put("rootTableName", rootName);
        result.put("nodes", nodes);
        // LineageGraph (frontend) reads 'links'; keep 'edges' as a compatibility alias.
        result.put("links", edges);
        result.put("edges", edges);
        result.put("totalNodes", nodes.size());
        result.put("totalEdges", edges.size());
        int up = (int) edges.stream().filter(e -> rootKey != null && rootKey.equals(e.get("target"))).count();
        int down = (int) edges.stream().filter(e -> rootKey != null && rootKey.equals(e.get("source"))).count();
        result.put("totalUpstream", up);
        result.put("totalDownstream", down);
        result.put("maxDepth", maxDepth != null && maxDepth > 0 ? maxDepth : 3);
        return result;
    }

    /**
     * BFS reachable edges within {@code depth} hops from {@code root} (anchored graph).
     */
    private List<LineageEntity> reachable(List<LineageEntity> edges, String root, int depth) {
        Set<String> visited = new LinkedHashSet<>();
        visited.add(root);
        Set<String> frontier = new LinkedHashSet<>();
        frontier.add(root);
        Set<String> edgeIds = new HashSet<>();
        List<LineageEntity> out = new ArrayList<>();
        for (int layer = 0; layer < depth && !frontier.isEmpty(); layer++) {
            Set<String> next = new LinkedHashSet<>();
            for (LineageEntity e : edges) {
                if (e.getSourceId() == null || e.getTargetId() == null) {
                    continue;
                }
                boolean sf = frontier.contains(e.getSourceId());
                boolean tf = frontier.contains(e.getTargetId());
                if (sf || tf) {
                    if (edgeIds.add(e.getId())) {
                        out.add(e);
                    }
                    if (sf && visited.add(e.getTargetId())) next.add(e.getTargetId());
                    if (tf && visited.add(e.getSourceId())) next.add(e.getSourceId());
                }
            }
            frontier = next;
        }
        return out;
    }

    /**
     * Get lineage overview statistics.
     */
    @Cacheable(value = CACHE_LINEAGE, unless = "#result == null || #result.isEmpty()")
    public Map<String, Object> getOverview() {
        List<LineageEntity> all = lineageRepository.findAll();
        List<LineageEntity> edges = all.stream().filter(e -> e.getSourceId() != null && e.getTargetId() != null).collect(Collectors.toList());
        long totalNodes = all.stream().map(LineageEntity::getSourceId).filter(Objects::nonNull).distinct().count();
        long totalEdges = edges.size();
        long hasCycle = edges.stream().filter(LineageEntity::isHasCycle).count();
        double avgDegree = totalNodes > 0 ? (double) (edges.size() * 2) / totalNodes : 0;

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalNodes", totalNodes);
        overview.put("totalEdges", totalEdges);
        overview.put("hasCycle", hasCycle > 0);
        overview.put("avgDegree", Math.round(avgDegree * 100.0) / 100.0);
        return overview;
    }

    /**
     * Impact analysis: find downstream nodes affected by a given node.
     */
    @Cacheable(value = CACHE_LINEAGE, unless = "#result == null || #result.isEmpty()")
    public Map<String, Object> impact(String nodeId, Integer maxDepth) {
        if (nodeId == null || nodeId.isEmpty()) {
            throw new BusinessException("nodeId is required");
        }
        int depth = maxDepth != null ? maxDepth : 5;
        List<LineageEntity> edges = lineageRepository.findAll().stream()
                .filter(e -> e.getSourceId() != null && e.getTargetId() != null)
                .collect(Collectors.toList());

        Set<String> visited = new LinkedHashSet<>();
        List<Map<String, Object>> impacted = new ArrayList<>();
        traverseDownstream(nodeId, edges, visited, impacted, 0, depth);

        // Resolve name
        String nodeName = resolveNodeName(nodeId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sourceId", nodeId);
        result.put("sourceName", nodeName);
        result.put("impacted", impacted);
        result.put("totalImpacted", impacted.size());
        return result;
    }

    /**
     * Lineage trace (upstream): find source nodes.
     */
    @Cacheable(value = CACHE_LINEAGE, unless = "#result == null || #result.isEmpty()")
    public Map<String, Object> lineage(String nodeId, Integer maxDepth) {
        if (nodeId == null || nodeId.isEmpty()) {
            throw new BusinessException("nodeId is required");
        }
        int depth = maxDepth != null ? maxDepth : 5;
        List<LineageEntity> edges = lineageRepository.findAll().stream()
                .filter(e -> e.getSourceId() != null && e.getTargetId() != null)
                .collect(Collectors.toList());

        Set<String> visited = new LinkedHashSet<>();
        List<Map<String, Object>> upstream = new ArrayList<>();
        traverseUpstream(nodeId, edges, visited, upstream, 0, depth);

        String nodeName = resolveNodeName(nodeId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodeId", nodeId);
        result.put("nodeName", nodeName);
        result.put("upstream", upstream);
        result.put("totalUpstream", upstream.size());
        return result;
    }

    /**
     * Find path between two nodes.
     */
    @Cacheable(value = CACHE_LINEAGE, unless = "#result == null || #result.isEmpty()")
    public Map<String, Object> findPath(Map<String, Object> body) {
        String source = (String) body.get("source");
        String target = (String) body.get("target");
        if (source == null || target == null) {
            throw new BusinessException("source and target are required");
        }

        List<LineageEntity> edges = lineageRepository.findAll().stream()
                .filter(e -> e.getSourceId() != null && e.getTargetId() != null)
                .collect(Collectors.toList());

        // BFS to find path
        Map<String, String> parent = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.add(source);
        visited.add(source);
        boolean found = false;

        while (!queue.isEmpty() && !found) {
            String current = queue.poll();
            for (LineageEntity e : edges) {
                if (current.equals(e.getSourceId()) && target.equals(e.getTargetId())) {
                    parent.put(target, current);
                    found = true;
                    break;
                }
                if (current.equals(e.getSourceId()) && !visited.contains(e.getTargetId())) {
                    visited.add(e.getTargetId());
                    parent.put(e.getTargetId(), current);
                    queue.add(e.getTargetId());
                }
            }
        }

        // Reconstruct path
        List<Map<String, Object>> path = new ArrayList<>();
        if (parent.containsKey(target) || source.equals(target)) {
            String current = target;
            while (!current.equals(source)) {
                String prev = parent.get(current);
                if (prev == null) break;
                Map<String, Object> step = new LinkedHashMap<>();
                step.put("source", prev);
                step.put("sourceName", resolveNodeName(prev));
                step.put("target", current);
                step.put("targetName", resolveNodeName(current));
                path.add(0, step);
                current = prev;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", source);
        result.put("target", target);
        result.put("found", !path.isEmpty());
        result.put("path", path);
        result.put("pathLength", path.size());
        return result;
    }

    /**
     * Detect cycles in the lineage graph.
     */
    @Cacheable(value = CACHE_LINEAGE, unless = "#result == null || #result.isEmpty()")
    public Map<String, Object> detectCycles() {
        List<LineageEntity> edges = lineageRepository.findAll().stream()
                .filter(e -> e.getSourceId() != null && e.getTargetId() != null)
                .collect(Collectors.toList());

        Set<String> allNodes = new HashSet<>();
        Map<String, List<String>> adjacency = new HashMap<>();
        for (LineageEntity e : edges) {
            allNodes.add(e.getSourceId());
            allNodes.add(e.getTargetId());
            adjacency.computeIfAbsent(e.getSourceId(), k -> new ArrayList<>()).add(e.getTargetId());
        }

        // DFS cycle detection
        Set<String> white = new HashSet<>(allNodes);
        Set<String> gray = new HashSet<>();
        Set<String> black = new HashSet<>();
        List<List<String>> cycles = new ArrayList<>();

        for (String node : allNodes) {
            if (white.contains(node)) {
                List<String> path = new ArrayList<>();
                if (dfsCycle(node, white, gray, black, adjacency, path, cycles)) {
                    // cycle detected
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasCycle", !cycles.isEmpty());
        result.put("cycleCount", cycles.size());
        result.put("cycles", cycles);
        return result;
    }

    private boolean dfsCycle(String node, Set<String> white, Set<String> gray, Set<String> black,
                             Map<String, List<String>> adj, List<String> path, List<List<String>> cycles) {
        white.remove(node);
        gray.add(node);
        path.add(node);

        for (String neighbor : adj.getOrDefault(node, Collections.emptyList())) {
            if (black.contains(neighbor)) continue;
            if (gray.contains(neighbor)) {
                // Found a cycle
                List<String> cycle = new ArrayList<>();
                int idx = path.indexOf(neighbor);
                for (int i = idx; i < path.size(); i++) {
                    cycle.add(path.get(i));
                }
                cycle.add(neighbor);
                cycles.add(cycle);
                return true;
            }
            if (dfsCycle(neighbor, white, gray, black, adj, path, cycles)) {
                return true;
            }
        }

        gray.remove(node);
        black.add(node);
        path.remove(path.size() - 1);
        return false;
    }

    /**
     * Heat analysis: compute node popularity based on in/out degree.
     */
    @Cacheable(value = CACHE_LINEAGE, unless = "#result == null || #result.isEmpty()")
    public List<Map<String, Object>> heatAnalysis() {
        List<LineageEntity> all = lineageRepository.findAll();
        Map<String, Integer> heatMap = new HashMap<>();

        for (LineageEntity e : all) {
            if (e.getSourceId() != null) {
                heatMap.merge(e.getSourceId(), 1, Integer::sum);
            }
            if (e.getTargetId() != null) {
                heatMap.merge(e.getTargetId(), 2, Integer::sum);
            }
        }

        List<Map<String, Object>> result = heatMap.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("nodeId", entry.getKey());
                    item.put("nodeName", resolveNodeName(entry.getKey()));
                    item.put("heat", entry.getValue());
                    item.put("inDegree", (int) all.stream().filter(e -> entry.getKey().equals(e.getTargetId())).count());
                    item.put("outDegree", (int) all.stream().filter(e -> entry.getKey().equals(e.getSourceId())).count());
                    return item;
                })
                .sorted((a, b) -> Integer.compare((int) b.get("heat"), (int) a.get("heat")))
                .collect(Collectors.toList());

        return result;
    }

    /**
     * Search lineage by keyword.
     */
    @Cacheable(value = CACHE_LINEAGE, unless = "#result == null || #result.isEmpty()")
    public List<Map<String, Object>> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String kw = keyword.toLowerCase();
        List<LineageEntity> matches = lineageRepository.findAll().stream()
                .filter(e -> (e.getName() != null && e.getName().toLowerCase().contains(kw))
                        || (e.getSourceName() != null && e.getSourceName().toLowerCase().contains(kw))
                        || (e.getTargetName() != null && e.getTargetName().toLowerCase().contains(kw))
                        || (e.getEdgeType() != null && e.getEdgeType().toLowerCase().contains(kw))
                        || (e.getSummary() != null && e.getSummary().toLowerCase().contains(kw)))
                .collect(Collectors.toList());

        // Return distinct entity NODES (table / task / report) rather than raw edges, so the
        // search box surfaces real tables (e.g. bigquery_marts.dws_sales_daily) that can be
        // selected as the anchor for "分析血缘".
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (LineageEntity e : matches) {
            addSearchNode(result, seen, e.getSourceId(), e.getSourceName(), e.getNodeType());
            addSearchNode(result, seen, e.getTargetId(), e.getTargetName(), e.getNodeType());
        }
        return result;
    }

    private void addSearchNode(List<Map<String, Object>> result, Set<String> seen,
                               String id, String name, String nodeType) {
        if (id == null || id.isBlank() || !seen.add(id)) {
            return;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("nodeId", id);
        item.put("name", name != null ? name : id);
        item.put("nodeType", nodeType != null ? nodeType : "TABLE");
        result.add(item);
    }

    /**
     * Export lineage data.
     */
    @Cacheable(value = CACHE_LINEAGE, unless = "#result == null || #result.isEmpty()")
    public Map<String, Object> export(String nodeType, String keyword) {
        return getGraph(nodeType, keyword, null, null);
    }

    private void traverseDownstream(String nodeId, List<LineageEntity> edges, Set<String> visited,
                                     List<Map<String, Object>> result, int depth, int maxDepth) {
        if (depth >= maxDepth || visited.contains(nodeId)) return;
        visited.add(nodeId);
        for (LineageEntity e : edges) {
            if (nodeId.equals(e.getSourceId()) && !visited.contains(e.getTargetId())) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("nodeId", e.getTargetId());
                node.put("nodeName", e.getTargetName());
                node.put("edgeType", e.getEdgeType());
                node.put("depth", depth + 1);
                result.add(node);
                traverseDownstream(e.getTargetId(), edges, visited, result, depth + 1, maxDepth);
            }
        }
    }

    private void traverseUpstream(String nodeId, List<LineageEntity> edges, Set<String> visited,
                                  List<Map<String, Object>> result, int depth, int maxDepth) {
        if (depth >= maxDepth || visited.contains(nodeId)) return;
        visited.add(nodeId);
        for (LineageEntity e : edges) {
            if (nodeId.equals(e.getTargetId()) && !visited.contains(e.getSourceId())) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("nodeId", e.getSourceId());
                node.put("nodeName", e.getSourceName());
                node.put("edgeType", e.getEdgeType());
                node.put("depth", depth + 1);
                result.add(node);
                traverseUpstream(e.getSourceId(), edges, visited, result, depth + 1, maxDepth);
            }
        }
    }

    private String resolveNodeName(String nodeId) {
        return lineageRepository.findAll().stream()
                .filter(e -> nodeId.equals(e.getSourceId()) || nodeId.equals(e.getTargetId()))
                .findFirst()
                .map(e -> nodeId.equals(e.getSourceId()) ? e.getSourceName() : e.getTargetName())
                .orElse(nodeId);
    }

    /**
     * 新增一条富语义血缘边（P0，设计文档 §3/§4）。将实体的来源枚举、审计字段
     * 与 {@link org.temporedata.api.gov.lineage.LineageDetails}（含列级 function /
     * SQL / 描述 / 审计）写入 temporedata_lineage 并持久化。
     *
     * @param sourceId 上游节点 ID
     * @param targetId 下游节点 ID
     * @param name     边名称
     * @param details  富语义详情（可为 null，此时仅写基本关系）
     * @return 已保存的 LineageEntity
     */
    @Transactional
    public LineageEntity saveEnriched(String sourceId, String targetId, String name,
                                      org.temporedata.api.gov.lineage.LineageDetails details) {
        LineageEntity le = LineageEntity.builder()
                .id(UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                .name(name)
                .nodeType("TABLE")
                .sourceId(sourceId).targetId(targetId)
                .edgeType("READ")
                .sourceSrs(details == null ? null : (details.getSource() == null ? null : details.getSource().name()))
                .lineageDetailsJson(serialize(details))
                .createdBy(details == null ? null : details.getCreatedBy())
                .createdAt(java.time.LocalDateTime.now())
                .build();
        return lineageRepository.save(le);
    }

    private String serialize(org.temporedata.api.gov.lineage.LineageDetails d) {
        if (d == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(d);
        } catch (Exception e) {
            log.warn("Failed to serialize LineageDetails, saved as null: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Create (or idempotently return) a rich lineage edge {@code from -> to}.
     * Both endpoints are authorized for edit, a per-edge distributed lock prevents
     * concurrent duplicates, and the dedicated graph cache is invalidated on success.
     */
    @CacheEvict(value = CACHE_LINEAGE, allEntries = true)
    @Transactional
    public LineageEntity saveEdge(String from, String to, LineageDetails details) {
        authHelper.authorizeLineageReference(from, to, true);
        enrichDefaults(details);

        if (!writeGate.tryLock(from, to)) {
            throw new BusinessException("并行操作冲突，请重试");
        }
        try {
            List<LineageEntity> existing = lineageRepository.findBySourceIdAndTargetId(from, to);
            if (!existing.isEmpty()) {
                return existing.get(0);
            }
            LineageEntity saved = saveEnriched(from, to, defaultEdgeName(from, to, details), details);
            graphCache.invalidateEdge(from, to);
            log.info("Saved lineage edge {} -> {} (source={})", from, to, details == null ? null : details.getSource());
            return saved;
        } finally {
            writeGate.unlock(from, to);
        }
    }

    /** Partial update of a rich edge: merges the incoming details onto the persisted JSON. */
    @CacheEvict(value = CACHE_LINEAGE, allEntries = true)
    @Transactional
    public LineageEdge patchEdge(String from, String to, LineageDetails patch) {
        authHelper.authorizeLineageReference(from, to, true);
        List<LineageEntity> edges = lineageRepository.findBySourceIdAndTargetId(from, to);
        if (edges.isEmpty()) {
            throw new BusinessException("边不存在: " + from + " -> " + to);
        }
        LineageEntity edge = edges.get(0);
        LineageDetails merged = lineageMapper.deserialize(edge.getLineageDetailsJson());
        if (merged == null) {
            merged = new LineageDetails();
        }
        applyPatch(merged, patch);
        merged.setUpdatedBy(authHelper.currentUserId());
        merged.setUpdatedAt(java.time.LocalDateTime.now());

        edge.setLineageDetailsJson(serialize(merged));
        edge.setSourceSrs(merged.getSource() != null ? merged.getSource().name() : edge.getSourceSrs());
        edge.setUpdatedBy(merged.getUpdatedBy());
        edge.setUpdatedAt(merged.getUpdatedAt());
        LineageEntity saved = lineageRepository.save(edge);
        graphCache.invalidateEdge(from, to);
        return lineageMapper.toEdge(saved);
    }

    /** Delete the single edge between the pair. */
    @CacheEvict(value = CACHE_LINEAGE, allEntries = true)
    @Transactional
    public void deleteEdge(String from, String to) {
        authHelper.authorizeLineageReference(from, to, true);
        List<LineageEntity> edges = lineageRepository.findBySourceIdAndTargetId(from, to);
        if (!edges.isEmpty()) {
            lineageRepository.deleteAll(edges);
        }
        graphCache.invalidateEdge(from, to);
        log.info("Deleted lineage edge {} -> {}", from, to);
    }

    /** Delete every edge of a given {@link Source} (e.g. clear all MANUAL). */
    @Transactional
    public long deleteBySource(Source source) {
        if (source == null) {
            throw new BusinessException("source is required");
        }
        List<LineageEntity> edges = lineageRepository.findBySourceSrs(source.name());
        lineageRepository.deleteAll(edges);
        if (!edges.isEmpty()) {
            graphCache.invalidateAll();
        }
        log.info("Deleted {} lineage edges of source {}", edges.size(), source);
        return edges.size();
    }

    /** Fetch a single edge's details, authorizing read on both endpoints. */
    public LineageEdge getEdge(String from, String to) {
        authHelper.authorizeLineageReference(from, to, false);
        List<LineageEntity> edges = lineageRepository.findBySourceIdAndTargetId(from, to);
        if (edges.isEmpty()) {
            throw new BusinessException("边不存在: " + from + " -> " + to);
        }
        return lineageMapper.toEdge(edges.get(0));
    }

    /** Sync helper: rebuild materialized edges and drop graph cache so reads.refresh. */
    @Transactional
    public List<LineageEntity> refresh() {
        graphCache.invalidateAll();
        return rebuild();
    }

    private void enrichDefaults(LineageDetails d) {
        if (d == null) {
            return;
        }
        if (d.getCreatedBy() == null || d.getCreatedBy().isBlank()) {
            d.setCreatedBy(authHelper.currentUserId());
        }
        if (d.getCreatedAt() == null) {
            d.setCreatedAt(java.time.LocalDateTime.now());
        }
    }

    private void applyPatch(LineageDetails target, LineageDetails patch) {
        if (patch == null) {
            return;
        }
        if (patch.getSource() != null) {
            target.setSource(patch.getSource());
        }
        if (patch.getSqlQuery() != null) {
            target.setSqlQuery(patch.getSqlQuery());
        }
        if (patch.getDescription() != null) {
            target.setDescription(patch.getDescription());
        }
        if (patch.getPipelineNodeId() != null) {
            target.setPipelineNodeId(patch.getPipelineNodeId());
        }
        if (patch.getColumnsLineage() != null) {
            target.setColumnsLineage(patch.getColumnsLineage());
        }
    }

    private String defaultEdgeName(String from, String to, LineageDetails d) {
        if (d != null && d.getDescription() != null && !d.getDescription().isBlank()) {
            return d.getDescription();
        }
        return from + " -> " + to;
    }
}