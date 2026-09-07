package org.temporedata.modules.dev.workflow.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Workflow run (instance) entity - one row per execution (P0-2 DAG runtime).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zy_wf_instance")
public class WorkflowInstanceEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "workflow_id", nullable = false, length = 36)
    private String workflowId;

    @Column(name = "task_instance_id", length = 36)
    private String taskInstanceId;

    @Column(name = "biz_date", length = 10)
    private String bizDate;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "trigger_type", nullable = false, length = 16)
    private String triggerType;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "finish_time")
    private LocalDateTime finishTime;

    @Column(name = "result_msg", columnDefinition = "TEXT")
    private String resultMsg;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false, length = 32)
    private String pool;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Column(name = "stopped_at")
    private LocalDateTime stoppedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}