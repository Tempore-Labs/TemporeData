package org.temporedata.modules.gov.lineage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.temporedata.api.gov.lineage.EntityLineage;
import org.temporedata.api.gov.lineage.EntityReference;
import org.temporedata.api.gov.lineage.SearchLineageResult;
import org.temporedata.modules.gov.lineage.assembler.LineageAssembler;
import org.temporedata.modules.gov.lineage.explorer.LineageGraphExplorer;
import org.temporedata.modules.gov.lineage.repository.LineageRepository;
import org.temporedata.modules.gov.lineage.security.LineageAuthHelper;
import org.temporedata.modules.gov.lineage.security.LineageDomainFilter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orchestrates the two lineage read paths (precise assembly + graph exploration) and
 * applies authorization + domain/tenant filtering before returning to the client.
 * Keeps {@link LineageService} focused on the write/model concerns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LineageExplorationService {

    private final LineageAssembler assembler;
    private final LineageGraphExplorer explorer;
    private final LineageDomainFilter domainFilter;
    private final LineageAuthHelper authHelper;
    private final LineageRepository lineageRepository;

    /** Precise depth-limited assembly for an entity. */
    public EntityLineage detail(String entityType, String id, Integer depth) {
        return domainFilter(assembler.assemble(entityType, id, depth));
    }

    public EntityLineage detailByFqn(String fqn, Integer depth) {
        return domainFilter(assembler.assembleByFqn(fqn, depth));
    }

    /** Layered graph exploration. */
    public SearchLineageResult explore(String entityId, LineageGraphExplorer.Direction direction,
                                       LocalDateTime timeStart, LocalDateTime timeEnd,
                                       int layerFrom, int layerSize) {
        SearchLineageResult raw = explorer.explore(entityId, direction, timeStart, timeEnd, layerFrom, layerSize);
        return domainFilter(raw);
    }

    public SearchLineageResult exploreDirection(LineageGraphExplorer.Direction direction, String entityId,
                                                int layerFrom, int layerSize) {
        return explore(entityId, direction, null, null, layerFrom, layerSize);
    }

    /** Batch hydrate a set of node references (resolve names), authorizing read on each. */
    public List<EntityReference> hydrate(List<EntityReference> roots) {
        List<EntityReference> out = new ArrayList<>();
        if (roots == null) {
            return out;
        }
        Map<String, String> nameIndex = nameIndex();
        for (EntityReference r : roots) {
            if (r == null || r.getId() == null) {
                continue;
            }
            authHelper.authorizeLineageReference(r.getId(), r.getId(), false);
            EntityReference resolved = new EntityReference();
            resolved.setId(r.getId());
            resolved.setName(r.getName() != null ? r.getName() : nameIndex.getOrDefault(r.getId(), r.getId()));
            resolved.setFqn(r.getFqn());
            resolved.setEntityType(r.getEntityType());
            out.add(resolved);
        }
        return out;
    }

    /** Quality-lineage overlay: a tenant-scoped node/edge projection for the DQ layer. */
    public SearchLineageResult dataQuality() {
        List<org.temporedata.modules.gov.lineage.entity.LineageEntity> rows = lineageRepository.findAll();
        SearchLineageResult result = new SearchLineageResult();
        Set<String> seenNodes = new LinkedHashSet<>();
        for (org.temporedata.modules.gov.lineage.entity.LineageEntity e : rows) {
            addNode(result, seenNodes, e.getSourceId(), e.getSourceName());
            addNode(result, seenNodes, e.getTargetId(), e.getTargetName());
        }
        result.setTotalNodes(result.getNodes().size());
        result.setTotalEdges(result.getEdges().size());
        return domainFilter(result);
    }

    /** CSV export of the reachable subgraph. */
    public String export(LineageGraphExplorer.Direction direction, String entityId,
                         LocalDateTime timeStart, LocalDateTime timeEnd) {
        return explorer.exportCsv(entityId, direction, timeStart, timeEnd);
    }

    // ---- helpers ----

    private EntityLineage domainFilter(EntityLineage el) {
        if (el == null) {
            return null;
        }
        return el;
    }

    private SearchLineageResult domainFilter(SearchLineageResult raw) {
        LineageDomainFilter.FilterResult filtered =
                domainFilter.filter(raw.getNodes(), raw.getEdges(), authHelper.current().getTenantId());
        raw.getNodes().clear();
        raw.getNodes().addAll(filtered.getNodes());
        raw.getEdges().clear();
        raw.getEdges().addAll(filtered.getEdges());
        raw.setHiddenCount(filtered.getHiddenCount());
        return raw;
    }

    private void addNode(SearchLineageResult result, Set<String> seen, String id, String name) {
        if (id == null || !seen.add(id)) {
            return;
        }
        EntityReference ref = new EntityReference();
        ref.setId(id);
        ref.setName(name != null ? name : id);
        ref.setEntityType("TABLE");
        result.getNodes().add(ref);
    }

    private Map<String, String> nameIndex() {
        Map<String, String> index = new LinkedHashMap<>();
        for (org.temporedata.modules.gov.lineage.entity.LineageEntity e : lineageRepository.findAll()) {
            if (e.getSourceId() != null) {
                index.putIfAbsent(e.getSourceId(), e.getSourceName());
            }
            if (e.getTargetId() != null) {
                index.putIfAbsent(e.getTargetId(), e.getTargetName());
            }
        }
        return index;
    }
}