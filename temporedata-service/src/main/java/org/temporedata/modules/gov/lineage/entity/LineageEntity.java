package org.temporedata.modules.gov.lineage.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Lineage entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "temporedata_lineage")
public class LineageEntity {

    @Id @GeneratedValue(generator = "uuid2") @GenericGenerator(name = "uuid2", strategy = "uuid2") @Column(length = 36)
    private String id;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createDateTime;

    @Column(length = 32)
    private String createBy;

    @UpdateTimestamp
    private LocalDateTime updateDateTime;

    @Column(length = 32)
    private String updateBy;

    @Column(length = 32)
    private String tenantId;
    @Column
    private boolean hasCycle;
    @Column
    private String summary;
    @Column
    private String name;
    @Column
    private String nodeType;
    @Column
    private int inDegree;
    @Column
    private int outDegree;
    @Column
    private int total;
    @Column
    private String sourceId;
    @Column
    private String targetId;
    @Column
    private String sourceName;
    @Column
    private String targetName;
    @Column
    private boolean found;
    @Column
    private String source;
    @Column
    private String target;
    @Column
    private String edgeType;
    @Column
    private String taskName;

    // ---- 富语义字段（设计文档 §3）：来源枚举 / 详细JSON / 审计 ----
    // 说明：与既有 zy_lineage 遗留的 source/target 字段（仅占位，见 rebuild）
    // 冲突，故来源枚举沿用独立列名 source_srs，与 V31__lineage_enhance.sql 对齐。
    @Column(name = "source_srs", length = 32)
    private String sourceSrs;

    @Column(name = "lineage_details_json", columnDefinition = "LONGTEXT")
    private String lineageDetailsJson;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
