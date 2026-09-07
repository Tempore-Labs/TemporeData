package org.temporedata.modules.dev.workflow.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/** Workflow entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_workflow")
public class WorkflowEntity {

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
    private String status;

    @Column(name = "schedule_cron")
    private String scheduleCron;

    @Column(name = "schedule_enabled")
    private Boolean scheduleEnabled;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String nodesJson;

    @Column(columnDefinition = "TEXT")
    private String edgesJson;

    @Column(name = "last_execute_time")
    private LocalDateTime lastExecuteTime;

    @Column(columnDefinition = "TEXT")
    private String executionsJson;
}