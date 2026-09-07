package org.temporedata.api.gov.lineage;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 大图探索（分层分页）结果，对应设计文档 §4.1 的 SearchLineageResult。
 * <p>由 {@code LineageGraphExplorer} 产出：返回一页节点/边，并在
 * {@code hiddenCount} 中报告因租户/域过滤被隐藏的节点数（按域分类），
 * 供前端提示"部分节点因权限未展示"。</p>
 */
@Data
public class SearchLineageResult {

    /** 本页节点。 */
    private List<EntityReference> nodes = new ArrayList<>();

    /** 本页边。 */
    private List<LineageEdge> edges = new ArrayList<>();

    /** 本页节点总数。 */
    private int totalNodes;

    /** 本页边总数。 */
    private int totalEdges;

    /** 按域/原因分类的隐藏节点计数（key 为域或 {@code tenant}）。 */
    private Map<String, Integer> hiddenCount = new LinkedHashMap<>();
}