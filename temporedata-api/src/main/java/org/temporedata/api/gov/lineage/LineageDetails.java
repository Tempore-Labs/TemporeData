package org.temporedata.api.gov.lineage;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 一条血缘边的附加（富语义）信息，对应设计文档 §3 的 LineageDetails。
 * 除“谁连谁”之外，还承载生成该血缘的 SQL、管道节点、来源枚举、
 * 列级 function 血缘及审计（创建/更新人及时毫秒级时间窗），用于大图按
 * 来源过滤、按时间窗硬剪枝与审计展示。
 */
@Data
public class LineageDetails {

    /** 生成该血缘的 SQL（可为空）。 */
    private String sqlQuery;

    /** 血缘描述。 */
    private String description;

    /** 来源枚举（对应 {@link Source}）。 */
    private Source source;

    /** 产生该边的管道/任务节点 ID。 */
    private String pipelineNodeId;

    /** 列级血缘（function(fromColumns[]) -> toColumn）。 */
    private List<ColumnLineageRecord> columnsLineage;

    /** 创建人。 */
    private String createdBy;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 最近更新人。 */
    private String updatedBy;

    /** 最近更新时间。 */
    private LocalDateTime updatedAt;
}