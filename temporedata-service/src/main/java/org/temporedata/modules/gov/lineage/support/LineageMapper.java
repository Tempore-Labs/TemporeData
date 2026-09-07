package org.temporedata.modules.gov.lineage.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.temporedata.api.gov.lineage.ColumnLineageRecord;
import org.temporedata.api.gov.lineage.EntityReference;
import org.temporedata.api.gov.lineage.LineageDetails;
import org.temporedata.api.gov.lineage.LineageEdge;
import org.temporedata.api.gov.lineage.Source;
import org.temporedata.modules.gov.lineage.entity.LineageEntity;

/**
 * Maps the persisted {@code temporedata_lineage} rows into the contract DTOs
 * ({@link EntityReference}, {@link LineageEdge}) and deserializes the enriched
 * {@code lineage_details_json} back into {@link LineageDetails}. Shared by the
 * precise assembler and the graph explorer so both read paths speak the same model.
 */
@Component
@RequiredArgsConstructor
public class LineageMapper {

    private final ObjectMapper objectMapper;

    public LineageEdge toEdge(LineageEntity e) {
        LineageEdge edge = new LineageEdge();
        edge.setId(e.getId());
        edge.setFrom(toNode(e.getSourceId(), e.getSourceName(), e.getNodeType()));
        edge.setTo(toNode(e.getTargetId(), e.getTargetName(), e.getNodeType()));
        edge.setType(e.getEdgeType());
        edge.setSource(parseSource(e.getSourceSrs()));
        // Propagate a stable engine key from the source so the frontend brand logo
        // mapping is deterministic (P3). Engine-like sources are real runtimes/dialects.
        String engine = engineOf(e.getSourceSrs());
        if (engine != null) {
            edge.getFrom().setEngine(engine);
            edge.getTo().setEngine(engine);
        }
        edge.setCreatedAt(e.getCreatedAt());
        edge.setUpdatedAt(e.getUpdatedAt());
        edge.setCreatedBy(e.getCreatedBy());
        edge.setUpdatedBy(e.getUpdatedBy());
        LineageDetails details = deserialize(e.getLineageDetailsJson());
        if (details != null) {
            edge.setDescription(details.getDescription());
            edge.setSqlQuery(details.getSqlQuery());
            edge.setColumnsLineage(details.getColumnsLineage());
            edge.setLineageDetails(details);
            if (details.getColumnsLineage() != null && !details.getColumnsLineage().isEmpty()) {
                ColumnLineageRecord first = details.getColumnsLineage().get(0);
                edge.setFunction(first.getFunction());
            }
        }
        return edge;
    }

    public EntityReference toNode(String id, String name, String nodeType) {
        EntityReference ref = new EntityReference();
        ref.setId(id);
        ref.setName(name != null ? name : id);
        ref.setEntityType(normalizeType(nodeType));
        return ref;
    }

    public LineageDetails deserialize(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LineageDetails.class);
        } catch (Exception ex) {
            return null;
        }
    }

    public Source parseSource(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        for (Source src : Source.values()) {
            if (src.name().equalsIgnoreCase(s)) {
                return src;
            }
        }
        return null;
    }

    /**
     * Map an engine-like lineage source to a stable engine key for the frontend brand
     * logo mapping. Only sources that are real runtimes/catalogs carry an engine; the
     * rest are injection origins (MANUAL/QUERY/PIPELINE/VIEW/...) and yield null so the
     * frontend falls back to the node database/name.
     */
    private String engineOf(String sourceSrs) {
        if (sourceSrs == null || sourceSrs.isBlank()) {
            return null;
        }
        switch (sourceSrs.toUpperCase()) {
            case "SPARK":
                return "spark";
            case "OPENLINEAGE":
                return "openlineage";
            case "DBT":
                return "dbt";
            default:
                return null;
        }
    }

    private String normalizeType(String nodeType) {
        if (nodeType == null || nodeType.isBlank()) {
            return "TABLE";
        }
        String t = nodeType.toUpperCase();
        switch (t) {
            case "COLUMN":
            case "FIELD":
                return "COLUMN";
            case "TASK":
            case "JOB":
                return "TASK";
            case "REPORT":
            case "DASHBOARD":
                return "REPORT";
            case "DQ_REPORT":
            case "DQ":
                return "DQ_REPORT";
            default:
                return "TABLE";
        }
    }
}