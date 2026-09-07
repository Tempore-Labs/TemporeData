package org.temporedata.modules.ops.ingestion.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Ingestion entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_ingestion")
public class IngestionEntity {

    @Id @GeneratedValue(generator = "uuid2") @GenericGenerator(name = "uuid2", strategy = "uuid2") @Column(length = 32)
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
    private String name;
    @Column
    private String type;
    @Column
    private String datasourceId;
    @Column
    private String targetTable;
    @Column
    private String config;
    @Column
    private String datasourceName;
    @Column
    private String status;
    @Column
    private Integer rowCount;
    @Column
    private String lastRunTime;
    @Column
    private String errorMsg;
    @Column
    private String createTime;
}
