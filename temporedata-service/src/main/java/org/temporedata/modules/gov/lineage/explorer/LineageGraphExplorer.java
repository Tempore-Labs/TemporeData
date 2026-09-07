package org.temporedata.modules.gov.lineage.explorer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.temporedata.api.gov.lineage.EntityReference;
import org.temporedata.api.gov.lineage.LineageEdge;
import org.temporedata.api.gov.lineage.SearchLineageResult;
import org.temporedata.modules.gov.lineage.config.LineageProperties;
import org.temporedata.modules.gov.lineage.entity.LineageEntity;
import org.temporedata.modules.gov.lineage.repository.LineageRepository;
import org.temporedata.modules.gov.lineage.support.LineageMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory layered BFS graph exploration (design doc §4.2 "图探索").
 * <p>Loads the (tenant + time-window pruned) edge set once, runs a layered BFS from a
 * root entity in the requested direction, assigns each reached node a layer and
 * returns a page of nodes/edges (layerFrom/layerSize pagination). The strategy selector
 * governs whether large graphs are paged rather than materialized whole to avoid OOM.
 * Also provides a bounded CSV export.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LineageGraphExplorer {

    public enum Direction { BOTH, UP, DOWN }

    private final LineageRepository lineageRepository;
    private final LineageMapper mapper;
    private final LineageStrategySelector strategySelector;
    private final LineageProperties properties;

    public SearchLineageResult explore(String rootId, Direction direction,
                                       LocalDateTime timeStart, LocalDateTime timeEnd) {
        return explore(rootId, direction, timeStart, timeEnd, 0, properties.getLayerSize());
    }

    /**
     * @param layerFrom zero-based node offset for pagination
     * @param layerSize  max nodes returned in this page
     */
    public SearchLineageResult explore(String rootId, Direction direction,
                                       LocalDateTime timeStart, LocalDateTime timeEnd,
                                       int layerFrom, int layerSize) {
        List<LineageEdge> pruned = prunedEdges(timeStart, timeEnd);
        long estNodes = lineageRepository.count();
        ExplorationStrategy strategy = strategySelector.select(estNodes);

        // Layered BFS: nodeId -> layer
        Map<String, Integer> nodeLayer = new LinkedHashMap<>();
        Map<String, LineageEdge> collected = new LinkedHashMap<>();
        bfs(rootId, direction, pruned, strategy, nodeLayer, collected);

        // Order nodes by (layer asc, id) for stable pagination.
        List<String> orderedIds = new ArrayList<>(nodeLayer.keySet());
        orderedIds.sort((a, b) -> {
            int byLayer = nodeLayer.get(a).compareTo(nodeLayer.get(b));
            return byLayer != 0 ? byLayer : a.compareTo(b);
        });
        List<EntityReference> allNodes = new ArrayList<>();
        for (String id : orderedIds) {
            EntityReference n = mapper.toNode(id, resolveName(id), null);
            n.setLayer(nodeLayer.get(id));
            allNodes.add(n);
        }
        List<LineageEdge> allEdges = new ArrayList<>(collected.values());

        int totalNodes = allNodes.size();
        int totalEdges = allEdges.size();

        // Apply optional pagination (default returns everything in one page).
        int size = layerSize > 0 ? layerSize : allNodes.size();
        int from = Math.max(0, layerFrom);
        List<EntityReference> pageNodes = from >= allNodes.size()
                ? List.of() : allNodes.subList(from, Math.min(from + size, allNodes.size()));
        List<LineageEdge> pageEdges = keepEdgesTouching(allEdges, pageNodes);

        SearchLineageResult result = new SearchLineageResult();
        result.getNodes().addAll(pageNodes);
        result.getEdges().addAll(pageEdges);
        result.setTotalNodes(totalNodes);
        result.setTotalEdges(totalEdges);
        return result;
    }

    /** Layered BFS collecting reached nodes (by layer) and the edges among them. */
    private void bfs(String rootId, Direction direction, List<LineageEdge> pruned,
                     ExplorationStrategy strategy, Map<String, Integer> nodeLayer,
                     Map<String, LineageEdge> collected) {
        if (!pruned.isEmpty() && reachable(rootId, pruned)) {
            nodeLayer.put(rootId, 0);
        }
        int cap = strategySelector.isPagedOnly(strategy) ? properties.getLayerSize() : -1;
        // index edges once
        Map<String, List<LineageEdge>> out = new LinkedHashMap<>();
        Map<String, List<LineageEdge>> in = new LinkedHashMap<>();
        for (LineageEdge e : pruned) {
            if (e.getFrom() != null && e.getFrom().getId() != null) {
                out.computeIfAbsent(e.getFrom().getId(), k -> new ArrayList<>()).add(e);
            }
            if (e.getTo() != null && e.getTo().getId() != null) {
                in.computeIfAbsent(e.getTo().getId(), k -> new ArrayList<>()).add(e);
            }
        }

        if (direction == Direction.UP || direction == Direction.BOTH) {
            expandLayered(rootId, true, out, in, nodeLayer, collected, cap);
        }
        if (direction == Direction.DOWN || direction == Direction.BOTH) {
            expandLayered(rootId, false, out, in, nodeLayer, collected, cap);
        }
    }

    private void expandLayered(String rootId, boolean upstream,
                               Map<String, List<LineageEdge>> out,
                               Map<String, List<LineageEdge>> in,
                               Map<String, Integer> nodeLayer,
                               Map<String, LineageEdge> collected, int cap) {
        List<String> frontier = new ArrayList<>();
        frontier.add(rootId);
        int layer = 0;
        while (!frontier.isEmpty()) {
            List<String> next = new ArrayList<>();
            for (String node : frontier) {
                List<LineageEdge> edges = upstream
                        ? in.getOrDefault(node, List.of())
                        : out.getOrDefault(node, List.of());
                for (LineageEdge e : edges) {
                    collected.putIfAbsent(e.getId(), e);
                    String neighbor = upstream ? e.getFrom().getId() : e.getTo().getId();
                    if (neighbor == null || neighbor.equals(rootId)) {
                        continue;
                    }
                    if (!nodeLayer.containsKey(neighbor)) {
                        nodeLayer.put(neighbor, layer + 1);
                        next.add(neighbor);
                    }
                }
            }
            frontier = next;
            layer++;
            if (cap > 0 && nodeLayer.size() >= cap) {
                if (upstream) {
                    log.debug("upstream exploration capped at {} nodes", cap);
                }
                break;
            }
        }
    }

    private boolean reachable(String rootId, List<LineageEdge> pruned) {
        for (LineageEdge e : pruned) {
            if (rootId.equals(e.getFrom().getId()) || rootId.equals(e.getTo().getId())) {
                return true;
            }
        }
        return false;
    }

    /** Time-window hard pruning: keep edge iff (createdAt<=end) && (updatedAt>=start); no-timestamp edges always match. */
    public boolean timeWindowMatch(LineageEntity e, LocalDateTime start, LocalDateTime end) {
        if (!properties.isTimeWindowEnabled()) {
            return true;
        }
        if (start == null && end == null) {
            return true;
        }
        if (e.getCreatedAt() != null && end != null && e.getCreatedAt().isAfter(end)) {
            return false;
        }
        if (e.getUpdatedAt() != null && start != null && e.getUpdatedAt().isBefore(start)) {
            return false;
        }
        return true;
    }

    private List<LineageEdge> prunedEdges(LocalDateTime timeStart, LocalDateTime timeEnd) {
        List<LineageEdge> out = new ArrayList<>();
        for (LineageEntity e : lineageRepository.findAll()) {
            if (timeWindowMatch(e, timeStart, timeEnd)) {
                out.add(mapper.toEdge(e));
            }
        }
        return out;
    }

    private List<LineageEdge> keepEdgesTouching(List<LineageEdge> allEdges, List<EntityReference> pageNodes) {
        Map<String, Boolean> present = new LinkedHashMap<>();
        for (EntityReference n : pageNodes) {
            present.put(n.getId(), Boolean.TRUE);
        }
        List<LineageEdge> out = new ArrayList<>();
        for (LineageEdge e : allEdges) {
            if (e.getFrom() != null && present.containsKey(e.getFrom().getId())
                    && e.getTo() != null && present.containsKey(e.getTo().getId())) {
                out.add(e);
            }
        }
        return out;
    }

    /** Bounded CSV export of the explored reachable subgraph (nodes + edges). */
    public String exportCsv(String rootId, Direction direction,
                            LocalDateTime timeStart, LocalDateTime timeEnd) {
        List<LineageEdge> pruned = prunedEdges(timeStart, timeEnd);
        Map<String, Integer> nodeLayer = new LinkedHashMap<>();
        Map<String, LineageEdge> collected = new LinkedHashMap<>();
        bfs(rootId, direction, pruned, ExplorationStrategy.LARGE, nodeLayer, collected);

        StringBuilder sb = new StringBuilder();
        sb.append("type,id,name,entityType,sourceName,targetName,edgeType,createdAt,updatedAt\n");
        List<String> orderedIds = new ArrayList<>(nodeLayer.keySet());
        orderedIds.sort((a, b) -> {
            int byLayer = nodeLayer.get(a).compareTo(nodeLayer.get(b));
            return byLayer != 0 ? byLayer : a.compareTo(b);
        });
        List<EntityReference> nodes = new ArrayList<>();
        for (String id : orderedIds) {
            nodes.add(mapper.toNode(id, resolveName(id), null));
        }
        for (EntityReference n : nodes) {
            sb.append("node,").append(csv(n.getId())).append(',').append(csv(n.getName()))
                    .append(',').append(csv(n.getEntityType()))
                    .append(",,,,").append('\n');
        }
        for (LineageEdge e : collected.values()) {
            sb.append("edge,")
                    .append(csv(e.getId())).append(",,")
                    .append(',').append(csv(e.getFrom() == null ? null : e.getFrom().getId()))
                    .append(',').append(csv(e.getTo() == null ? null : e.getTo().getId()))
                    .append(',').append(csv(e.getType()))
                    .append(',').append(e.getCreatedAt()).append(',')
                    .append(e.getUpdatedAt()).append('\n');
        }
        return sb.toString();
    }

    private String resolveName(String id) {
        return lineageRepository.findAll().stream()
                .filter(e -> id.equals(e.getSourceId()) || id.equals(e.getTargetId()))
                .findFirst()
                .map(e -> id.equals(e.getSourceId()) ? e.getSourceName() : e.getTargetName())
                .orElse(id);
    }

    private String csv(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}