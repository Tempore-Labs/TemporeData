package org.temporedata.modules.gov.lineage.controller;

import org.temporedata.api.gov.lineage.EntityLineage;
import org.temporedata.api.gov.lineage.EntityReference;
import org.temporedata.api.gov.lineage.LineageDetails;
import org.temporedata.api.gov.lineage.LineageEdge;
import org.temporedata.api.gov.lineage.SearchLineageResult;
import org.temporedata.api.gov.lineage.Source;
import org.temporedata.modules.gov.lineage.entity.LineageEntity;
import org.temporedata.modules.gov.lineage.explorer.LineageGraphExplorer;
import org.temporedata.modules.gov.lineage.service.LineageExplorationService;
import org.temporedata.modules.gov.lineage.service.LineageService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lineage")
@RequiredArgsConstructor
public class LineageController {

    private final LineageService lineageService;
    private final LineageExplorationService explorationService;

    @PostMapping("/rebuild")
    public BaseResponse<List<LineageEntity>> rebuild() {
        return BaseResponse.success(lineageService.rebuild());
    }

    @GetMapping("/graph")
    public BaseResponse<Map<String, Object>> getGraph(@RequestParam(required = false) String nodeType,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Integer maxDepth,
                                                      @RequestParam(required = false) String nodeId) {
        return BaseResponse.success(lineageService.getGraph(nodeType, keyword, maxDepth, nodeId));
    }

    @GetMapping("/overview")
    public BaseResponse<Map<String, Object>> getOverview() {
        return BaseResponse.success(lineageService.getOverview());
    }

    @GetMapping("/impact")
    public BaseResponse<Map<String, Object>> impact(@RequestParam String nodeId,
                                                    @RequestParam(required = false) Integer maxDepth) {
        return BaseResponse.success(lineageService.impact(nodeId, maxDepth));
    }

    @GetMapping("/lineage")
    public BaseResponse<Map<String, Object>> lineage(@RequestParam String nodeId,
                                                     @RequestParam(required = false) Integer maxDepth) {
        return BaseResponse.success(lineageService.lineage(nodeId, maxDepth));
    }

    @PostMapping("/path")
    public BaseResponse<Map<String, Object>> findPath(@RequestBody Map<String, Object> body) {
        return BaseResponse.success(lineageService.findPath(body));
    }

    @GetMapping("/cycles")
    public BaseResponse<Map<String, Object>> detectCycles() {
        return BaseResponse.success(lineageService.detectCycles());
    }

    @GetMapping("/heat")
    public BaseResponse<List<Map<String, Object>>> heatAnalysis() {
        return BaseResponse.success(lineageService.heatAnalysis());
    }

    @GetMapping("/search")
    public BaseResponse<List<Map<String, Object>>> search(@RequestParam String keyword) {
        return BaseResponse.success(lineageService.search(keyword));
    }

    @GetMapping("/export")
    public BaseResponse<Map<String, Object>> export(@RequestParam(required = false) String nodeType,
                                                    @RequestParam(required = false) String keyword) {
        return BaseResponse.success(lineageService.export(nodeType, keyword));
    }

    // ------------------------------------------------------------------
    // Enriched lineage API (P0/P1): precise assembly, exploration, write.
    // Existing endpoints above remain as compatibility aliases.
    // ------------------------------------------------------------------

    /** Precise depth-limited assembly by entity type + id. */
    @GetMapping("/{entityType}/{id}")
    public BaseResponse<EntityLineage> detail(@PathVariable String entityType,
                                              @PathVariable String id,
                                              @RequestParam(required = false) Integer depth) {
        return BaseResponse.success(explorationService.detail(entityType, id, depth));
    }

    /** Precise assembly by fully-qualified name. */
    @GetMapping("/name/{fqn}")
    public BaseResponse<EntityLineage> detailByFqn(@PathVariable String fqn,
                                                   @RequestParam(required = false) Integer depth) {
        return BaseResponse.success(explorationService.detailByFqn(fqn, depth));
    }

    /** Layered graph exploration (up/`LayerFrom'/`LayerSize' pagination + time window). */
    @GetMapping("/explore")
    public BaseResponse<SearchLineageResult> explore(@RequestParam String entityId,
                                                     @RequestParam(required = false) String direction,
                                                     @RequestParam(required = false, defaultValue = "0") int layerFrom,
                                                     @RequestParam(required = false, defaultValue = "200") int layerSize,
                                                     @RequestParam(required = false) Long timeStart,
                                                     @RequestParam(required = false) Long timeEnd) {
        return BaseResponse.success(explorationService.explore(entityId,
                parseDirection(direction), toLocal(timeStart), toLocal(timeEnd), layerFrom, layerSize));
    }

    /** Direction-specific exploration. */
    @GetMapping("/explore/{direction}")
    public BaseResponse<SearchLineageResult> exploreDirection(@PathVariable String direction,
                                                              @RequestParam String entityId,
                                                              @RequestParam(required = false, defaultValue = "0") int layerFrom,
                                                              @RequestParam(required = false, defaultValue = "200") int layerSize) {
        return BaseResponse.success(explorationService.exploreDirection(
                parseDirection(direction), entityId, layerFrom, layerSize));
    }

    /** Batch hydrate node references. */
    @PostMapping("/hydrate")
    public BaseResponse<List<EntityReference>> hydrate(@RequestBody List<EntityReference> roots) {
        return BaseResponse.success(explorationService.hydrate(roots));
    }

    /** Single edge details (rich: sql / columns / source / audit). */
    @GetMapping("/edge/{from}/{to}")
    public BaseResponse<LineageEdge> getEdge(@PathVariable String from, @PathVariable String to) {
        return BaseResponse.success(lineageService.getEdge(from, to));
    }

    /** Quality-lineage overlay graph. */
    @GetMapping("/dataquality")
    public BaseResponse<SearchLineageResult> dataQuality() {
        return BaseResponse.success(explorationService.dataQuality());
    }

    /** CSV export of the reachable subgraph. */
    @GetMapping("/export/csv")
    public BaseResponse<String> exportCsv(@RequestParam String entityId,
                                          @RequestParam(required = false) String direction,
                                          @RequestParam(required = false) Long timeStart,
                                          @RequestParam(required = false) Long timeEnd) {
        return BaseResponse.success(explorationService.export(
                parseDirection(direction), entityId, toLocal(timeStart), toLocal(timeEnd)));
    }

    /** Create (or idempotently return) a rich edge. */
    @PutMapping("/edge/{from}/{to}")
    public BaseResponse<LineageEntity> saveEdge(@PathVariable String from, @PathVariable String to,
                                                @RequestBody(required = false) LineageDetails details) {
        return BaseResponse.success(lineageService.saveEdge(from, to, details));
    }

    /** Partially update a rich edge. */
    @PatchMapping("/edge/{from}/{to}")
    public BaseResponse<LineageEdge> patchEdge(@PathVariable String from, @PathVariable String to,
                                               @RequestBody(required = false) LineageDetails details) {
        return BaseResponse.success(lineageService.patchEdge(from, to, details));
    }

    /** Delete a single edge. */
    @DeleteMapping("/edge/{from}/{to}")
    public BaseResponse<Void> deleteEdge(@PathVariable String from, @PathVariable String to) {
        lineageService.deleteEdge(from, to);
        return BaseResponse.success(null);
    }

    /** Delete all edges of a given {@link Source} (e.g. clear MANUAL). */
    @DeleteMapping("/type/{source}")
    public BaseResponse<Long> deleteBySource(@PathVariable String source) {
        return BaseResponse.success(lineageService.deleteBySource(Source.valueOf(source)));
    }

    /** Actively rebuild materialized edges and drop caches. */
    @PostMapping("/refresh")
    public BaseResponse<List<LineageEntity>> refresh() {
        return BaseResponse.success(lineageService.refresh());
    }

    private LineageGraphExplorer.Direction parseDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return LineageGraphExplorer.Direction.BOTH;
        }
        switch (direction.toUpperCase()) {
            case "UP":
                return LineageGraphExplorer.Direction.UP;
            case "DOWN":
                return LineageGraphExplorer.Direction.DOWN;
            default:
                return LineageGraphExplorer.Direction.BOTH;
        }
    }

    private LocalDateTime toLocal(Long millis) {
        return millis == null ? null
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }
}