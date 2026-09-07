package org.temporedata.api.gov.lineage;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 血缘边，对应设计文档 §3 的 LineageEdge（参考 OpenMetadata 写接口）。
 * <p>一条边承载"谁连谁"（{@code from} → {@code to}）以及富语义信息：
 * 来源枚举、生成 SQL、列级 function 血缘、描述与审计时间窗。{@code lineageDetails}
 * 是完整 JSON 载荷（可空），其余字段为其扁平化视图便于轻量展示。</p>
 */
@Data
public class LineageEdge {

    /** 边 ID（temporedata_lineage 主键）。 */
    private String id;

    /** 上游（来源）端点。 */
    private EntityReference from;

    /** 下游（目标）端点。 */
    private EntityReference to;

    /** 边类型：READ / WRITE / MANUAL 等。 */
    private String type;

    /** 来源枚举（可空）。 */
    private Source source;

    /** 描述。 */
    private String description;

    /** 生成该血缘的 SQL。 */
    private String sqlQuery;

    /** 列级血缘。 */
    private List<ColumnLineageRecord> columnsLineage;

    /** 列级 function（目标列推导函数，可空）。 */
    private String function;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 最近更新时间。 */
    private LocalDateTime updatedAt;

    /** 创建人。 */
    private String createdBy;

    /** 最近更新人。 */
    private String updatedBy;

    /** 完整富语义详情（可空）。 */
    private LineageDetails lineageDetails;
}