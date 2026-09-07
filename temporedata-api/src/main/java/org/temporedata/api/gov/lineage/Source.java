package org.temporedata.api.gov.lineage;

/**
 * 血缘边（LineageEdge）的来源枚举，对应设计文档 §3 的富语义边模型。
 * 标识一条血缘关系是如何产生/注入的，例如手动编辑连线（MANUAL）、
 * 数据视图（VIEW）、SQL 查询解析（QUERY）、管道/任务（PIPELINE）、
 * DBT / Spark / OpenLineage 采集、外部表或跨库引用、报表（DASHBOARD）。
 * 平台侧可用该枚举做来源过滤与按来源批量删除。
 */
public enum Source {
    MANUAL,
    VIEW,
    QUERY,
    PIPELINE,
    DBT,
    SPARK,
    OPENLINEAGE,
    EXTERNAL_TABLE,
    CROSS_DATABASE,
    DASHBOARD
}