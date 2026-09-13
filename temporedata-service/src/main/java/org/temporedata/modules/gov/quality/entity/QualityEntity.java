package org.temporedata.modules.gov.quality.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Quality entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_quality")
public class QualityEntity {

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
    private String checkId;
    @Column
    private String ruleName;
    @Column
    private String status;
    @Column
    private String result;
    @Column
    private long durationMs;
    @Column
    private String errorMsg;
    @Column
    private String checkTime;
    @Column
    private String name;
    @Column
    private String datasourceId;
    @Column
    private String tableName;
    @Column
    private String columnName;
    @Column
    private String ruleType;
    @Column
    private String ruleConfig;
    @Column
    private String description;
    @Column
    private String datasourceName;
    @Column
    private String lastStatus;
    @Column
    private String createTime;
}
