package org.temporedata.api.gov.lineage;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 血缘查询结果（根实体视图），对应设计文档 §3 的 EntityLineage。
 * <p>由 {@code LineageAssembler} 精确组装产生：围绕一个根实体（{@code entity}），
 * 给出上游与下游边列表及计数，深度受控（默认 ≤3）。相互独立的
 * 上游/下游是无环展开的，展示为"上游左、下游右"的分层视图。</p>
 */
@Data
public class EntityLineage {

    /** 根实体引用。 */
    private EntityReference entity;

    /** 根实体上游边（上游 → 根）。 */
    private List<LineageEdge> upstream = new ArrayList<>();

    /** 根实体下游边（根 → 下游）。 */
    private List<LineageEdge> downstream = new ArrayList<>();

    /** 本次组装的深度。 */
    private int maxDepth;

    /** 上游边总数。 */
    private long totalUpstream;

    /** 下游边总数。 */
    private long totalDownstream;
}