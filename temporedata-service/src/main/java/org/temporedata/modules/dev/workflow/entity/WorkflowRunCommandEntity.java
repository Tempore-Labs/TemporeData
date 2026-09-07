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
 * Auditable runtime command (pause / resume / stop / rerun) for a workflow
 * instance (P0-3 runtime management).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zy_wf_run_command")
public class WorkflowRunCommandEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "instance_id", nullable = false, length = 36)
    private String instanceId;

    /** Pause | Resume | Stop | Rerun. */
    @Column(nullable = false, length = 16)
    private String type;

    /** ALL | NODE. */
    @Column(nullable = false, length = 16)
    private String scope;

    @Column(name = "target_node_id", length = 64)
    private String targetNodeId;

    /** PENDING | DONE | REJECTED. */
    @Column(nullable = false, length = 16)
    private String state;

    @Column(length = 64)
    private String operator;

    @Column(length = 255)
    private String reason;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "done_time")
    private LocalDateTime doneTime;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}