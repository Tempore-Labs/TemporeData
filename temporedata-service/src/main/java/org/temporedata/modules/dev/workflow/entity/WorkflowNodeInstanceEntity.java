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
 * Workflow node instance entity - per-node status within a run (P0-2 DAG runtime).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zy_wf_node_instance")
public class WorkflowNodeInstanceEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "instance_id", nullable = false, length = 36)
    private String instanceId;

    @Column(name = "node_id", nullable = false, length = 64)
    private String nodeId;

    @Column(name = "node_name", length = 128)
    private String nodeName;

    @Column(name = "node_type", length = 32)
    private String nodeType;

    /** PENDING | RUNNING | SUCCESS | FAILED | SKIPPED | PAUSED | STOPPED. */
    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "finish_time")
    private LocalDateTime finishTime;

    @Column(name = "retry_times", nullable = false)
    private Integer retryTimes;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;

    @Column(name = "duration_ms")
    private Long durationMs;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}