package org.temporedata.api.gov.lineage;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 血缘图中的节点（实体）引用，对应设计文档 §3 的 EntityReference。
 * <p>表达一个血缘节点：既可以是表/任务/报表，也可以是列/DQ 报告等；携带
 * 展示所需的基本身份信息与可选的扩展属性。{@code layer} 由探索器在分层遍历
 * 时赋值（0 起）；{@code hidden} 表示该节点因租户/域过滤被隐藏但仍被引用。</p>
 */
@Data
public class EntityReference {

    /** 血缘节点 ID（如 {@code tbl:ods_orders}）。 */
    private String id;

    /** 展示名称。 */
    private String name;

    /** 全限定名（如 {@code td.ods.orders}）。 */
    private String fqn;

    /** 实体类型：TABLE / COLUMN / TASK / REPORT / DQ_REPORT。 */
    private String entityType;

    /** 所属库。 */
    private String database;

    /** 所属 schema。 */
    private String schema;

    /** 规范化数据源引擎标识（如 mysql / bigquery / spark / hive），用于前端品牌 Logo 稳定映射；
     *  由血缘采集来源推导，未知时为 null（前端回退到 database / 名称首字母）。 */
    private String engine;

    /** 表名（列级可为表名）。 */
    private String table;

    /** 扩展属性（可选：rowCount / owner / tags 等）。 */
    private Map<String, Object> attributes = new LinkedHashMap<>();

    /** 是否因租户/域过滤被隐藏。 */
    private boolean hidden;

    /** 探索时所在层（0 起，可选）。 */
    private Integer layer;
}