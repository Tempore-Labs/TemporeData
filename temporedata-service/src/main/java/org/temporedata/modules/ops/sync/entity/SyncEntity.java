package org.temporedata.modules.ops.sync.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Sync entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_sync")
public class SyncEntity {

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
    private String sourceDatasourceId;
    @Column
    private String sourceTable;
    @Column
    private String targetDatasourceId;
    @Column
    private String targetTable;
    @Column
    private String syncMode;
    @Column
    private String incrementalColumn;
    @Column
    private String incrementalValue;
    @Column
    private String batchSize;
    @Column
    private String config;
    @Column
    private String status;
    @Column
    private Integer rowCount;
    @Column
    private String lastRunTime;
    @Column
    private String duration;
    @Column
    private String errorMsg;
    @Column
    private String createTime;
}
