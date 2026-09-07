package org.temporedata.modules.asset.indicator.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Indicator entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_indicator")
public class IndicatorEntity {

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
    private String code;
    @Column
    private String type;
    @Column
    private String status;
    @Column
    private long total;
    @Column
    private String description;
    @Column
    private String querySql;
    @Column
    private String datasourceId;
    @Column
    private String unit;
    @Column
    private String version;
    @Column
    private String theme;
    @Column
    private String owner;
    @Column
    private String scheduleCron;
    @Column
    private Boolean scheduleEnabled;
    @Column
    private String createTime;
    @Column
    private String updateTime;
    @Column
    private String latestStatus;
    @Column
    private String latestValue;
    @Column
    private String latestExecuteTime;
    @Column
    private Long latestDurationMs;
    @Column
    private String latestErrorMsg;
    @Column
    private String indicatorId;
    @Column
    private String resultValue;
    @Column
    private Long durationMs;
    @Column
    private String errorMsg;
    @Column
    private String executeTime;
    @Column
    private int totalRuns;
    @Column
    private int successRuns;
    @Column
    private int failedRuns;
}
