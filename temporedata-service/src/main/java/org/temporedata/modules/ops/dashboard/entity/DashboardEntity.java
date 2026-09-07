package org.temporedata.modules.ops.dashboard.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Dashboard entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_dashboard")
public class DashboardEntity {

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

    /** Overall stats */
    @Column
    private int datasourceCount;
    @Column
    private int tableCount;
    @Column
    private long totalRows;
    @Column
    private int syncTaskCount;
    @Column
    private int syncSuccessCount;
    @Column
    private int syncFailCount;
    @Column
    private int qualityRuleCount;
    @Column
    private int qualityPassCount;
    @Column
    private int qualityFailCount;
    @Column
    private int workflowCount;
    @Column
    private int apiCount;
    @Column
    private long apiCallTotal;

    /** Datasource list (name/type) */
    @Column
    private String name;
    @Column
    private int value;
    @Column
    private String tableName;
    @Column
    private long rowCount;
    @Column
    private String taskName;
    @Column
    private String status;
    @Column
    private String lastRunTime;
    @Column
    private String hour;
    @Column
    private int success;
    @Column
    private int failed;
    @Column
    private String targetTable;
    @Column
    private String errorMsg;
    @Column
    private String ruleName;
    @Column
    private String columnName;
    @Column
    private String ruleType;
    @Column
    private String description;
    @Column
    private String type;
}
