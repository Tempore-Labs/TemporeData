package org.temporedata.modules.gov.lineage.assembler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.gov.lineage.EntityLineage;
import org.temporedata.api.gov.lineage.EntityReference;
import org.temporedata.api.gov.lineage.LineageEdge;
import org.temporedata.modules.gov.lineage.config.LineageProperties;
import org.temporedata.modules.gov.lineage.entity.LineageEntity;
import org.temporedata.modules.gov.lineage.repository.LineageRepository;
import org.temporedata.modules.gov.lineage.security.LineageAuthHelper;
import org.temporedata.modules.gov.lineage.support.LineageMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Precise depth-limited lineage assembly (design doc §4.2 "精确组装").
 * <p>Traverses {@code temporedata_lineage} up/down from a single root entity using
 * batched in-graph BFS (no full-table load beyond the reached layers), hydrates
 * neighbor references and returns an {@link EntityLineage} whose depth is capped
 * (default ≤3 from {@link LineageProperties}). This is the "short, precise" read path;
 * the exploration engine handles large graphs.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LineageAssembler {

    private final LineageRepository lineageRepository;
    private final LineageMapper mapper;
    private final LineageAuthHelper authHelper;
    private final LineageProperties properties;

    public EntityLineage assemble(String entityType, String id, Integer depth) {
        authHelper.authorizeLineageReference(id, id, false);
        String rootName = resolveName(id);
        EntityReference root = mapper.toNode(id, rootName, entityType);
        int depthLimit = depth != null
                ? depth
                : Math.max(properties.getUpstreamDepth(), properties.getDownstreamDepth());
        return assembleFrom(root, depthLimit);
    }

    public EntityLineage assembleByFqn(String fqn, Integer depth) {
        String seedId = resolveByFqn(fqn);
        if (seedId == null) {
            throw new BusinessException("未找到血缘节点: " + fqn);
        }
        return assemble(null, seedId, depth);
    }

    private EntityLineage assembleFrom(EntityReference root, int depthLimit) {
        EntityLineage result = new EntityLineage();
        result.setEntity(root);
        result.getUpstream().addAll(collect(root.getId(), true, depthLimit));
        result.getDownstream().addAll(collect(root.getId(), false, depthLimit));
        result.setMaxDepth(depthLimit);
        result.setTotalUpstream(countNodes(result.getUpstream(), false));
        result.setTotalDownstream(countNodes(result.getDownstream(), true));
        return result;
    }

    /**
     * Batched layered BFS. When {@code upstream=true} it walks root &lt;- ... (edges whose
     * {@code targetId} is in the frontier, next frontier = their {@code sourceId}); when
     * {@code upstream=false} it walks root -&gt; ... (edges whose {@code sourceId} is in the
     * frontier, next frontier = their {@code targetId}).
     */
    private List<LineageEdge> collect(String rootId, boolean upstream, int depthLimit) {
        List<LineageEdge> collected = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(rootId);

        Set<String> frontier = new LinkedHashSet<>();
        frontier.add(rootId);
        for (int layer = 1; layer <= depthLimit && !frontier.isEmpty(); layer++) {
            List<LineageEntity> batch = upstream
                    ? lineageRepository.findByTargetIdIn(frontier)
                    : lineageRepository.findBySourceIdIn(frontier);

            Set<String> next = new LinkedHashSet<>();
            for (LineageEntity e : batch) {
                String neighbor = upstream ? e.getSourceId() : e.getTargetId();
                if (neighbor == null || visited.contains(neighbor)) {
                    continue;
                }
                collected.add(mapper.toEdge(e));
                next.add(neighbor);
                visited.add(neighbor);
            }
            frontier = next;
        }
        return collected;
    }

    private long countNodes(List<LineageEdge> edges, boolean farSide) {
        Set<String> ids = new HashSet<>();
        for (LineageEdge e : edges) {
            EntityReference ref = farSide ? e.getTo() : e.getFrom();
            if (ref != null && ref.getId() != null) {
                ids.add(ref.getId());
            }
        }
        return ids.size();
    }

    private String resolveName(String id) {
        if (id == null) {
            return null;
        }
        Optional<LineageEntity> any = lineageRepository.findAll().stream()
                .filter(e -> id.equals(e.getSourceId()) || id.equals(e.getTargetId()))
                .findFirst();
        if (any.isPresent()) {
            LineageEntity e = any.get();
            return id.equals(e.getSourceId()) ? e.getSourceName() : e.getTargetName();
        }
        return id;
    }

    private String resolveByFqn(String fqn) {
        if (fqn == null || fqn.isBlank()) {
            return null;
        }
        // FQN usually matches the node id (e.g. tbl:ods.orders); name scan is a fallback.
        return lineageRepository.findAll().stream()
                .filter(e -> fqn.equals(e.getSourceId()) || fqn.equals(e.getTargetId()))
                .map(e -> fqn.equals(e.getSourceId()) ? e.getSourceName() : e.getTargetName())
                .findFirst()
                .orElseGet(() -> lineageRepository.findAll().stream()
                        .filter(e -> fqn.equalsIgnoreCase(e.getSourceName())
                                || fqn.equalsIgnoreCase(e.getTargetName()))
                        .map(e -> fqn.equalsIgnoreCase(e.getSourceName())
                                ? e.getSourceId() : e.getTargetId())
                        .findFirst()
                        .orElse(null));
    }
}