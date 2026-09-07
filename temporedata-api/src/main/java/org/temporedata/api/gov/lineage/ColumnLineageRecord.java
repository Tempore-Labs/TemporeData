package org.temporedata.api.gov.lineage;

import lombok.Data;

import java.util.List;

/**
 * 列级血缘记录，对应设计文档 §3 的富语义列级 function 模型。
 * 描述一个目标列（toColumn）由其上游一组源列（fromColumns）经某个函数
 * （function）推导而来，例如 {@code concat(a, b) -> c}。
 */
@Data
public class ColumnLineageRecord {

    /** 上游源列（可多个，格式通常为 表名.列名）。 */
    private List<String> fromColumns;

    /** 目标列（本表列）。 */
    private String toColumn;

    /** 生成该目标列所用函数/表达式（可为空，表示直接映射）。 */
    private String function;
}