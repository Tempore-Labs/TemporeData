package org.temporedata.modules.gov.lineage.repository;

import org.temporedata.modules.gov.lineage.entity.LineageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Repository for {@code temporedata_lineage}.
 * <p>The lineage graph is stored as materialized edges in a single table; the
 * traversal helpers below support batched layered BFS (upstream via
 * {@code targetId}, downstream via {@code sourceId}), enriched-field queries and
 * time-window candidate scans without recursive SQL.</p>
 */
@Repository
public interface LineageRepository extends JpaRepository<LineageEntity, String> {

    /** Edges whose target is in {@code ids} (used for upstream layered BFS). */
    List<LineageEntity> findByTargetIdIn(Collection<String> ids);

    /** Edges whose source is in {@code ids} (used for downstream layered BFS). */
    List<LineageEntity> findBySourceIdIn(Collection<String> ids);

    /** Edges pointing to the given target within a tenant. */
    List<LineageEntity> findByTargetIdAndTenantId(String targetId, String tenantId);

    /** Edges sourced from the given source within a tenant. */
    List<LineageEntity> findBySourceIdAndTenantId(String sourceId, String tenantId);

    /** Edges by source enum + edge type (source/type filter & bulk delete). */
    List<LineageEntity> findBySourceSrsAndEdgeType(String sourceSrs, String edgeType);

    /** Edges by source enum (bulk delete / filter across all edge types). */
    List<LineageEntity> findBySourceSrs(String sourceSrs);

    /** Whether an edge between the pair already exists (concurrency dedupe). */
    long countBySourceIdAndTargetId(String sourceId, String targetId);

    /** The specific edge from source to target (write/patch/delete target). */
    List<LineageEntity> findBySourceIdAndTargetId(String sourceId, String targetId);

    /** Edges updated after a timestamp (time-window candidate scan). */
    List<LineageEntity> findByUpdatedAtAfter(LocalDateTime since);

    @Query("select e from LineageEntity e where e.sourceId = :s or e.targetId = :t")
    List<LineageEntity> edgesTouching(@Param("s") String s, @Param("t") String t);

    @Query("select distinct e.sourceSrs from LineageEntity e where e.sourceSrs is not null")
    List<String> distinctSourceSrs();
}