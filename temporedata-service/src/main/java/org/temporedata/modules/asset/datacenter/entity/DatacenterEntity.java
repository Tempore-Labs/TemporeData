package org.temporedata.modules.asset.datacenter.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Datacenter entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_datacenter")
public class DatacenterEntity {

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
    private long datasourceCount;
    @Column
    private long tableCount;
    @Column
    private long workflowCount;
    @Column
    private long apiCount;
    @Column
    private long syncTaskCount;
    @Column
    private long qualityRuleCount;
    @Column
    private String name;
    @Column
    private String description;
    @Column
    private String category;
    @Column
    private String config;
    @Column
    private String datasourceId;
    @Column
    private Integer refreshInterval;
    @Column
    private String status;
    @Column
    private String createTime;
    @Column
    private String updateTime;
}
