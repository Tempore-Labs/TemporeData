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
 * Per-node table dependency of a workflow (P1-5 task-level lineage).
 * source_table = a table the node reads; target_table = a table it produces.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "temporedata_workflow_lineage")
public class WorkflowLineageEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "workflow_id", nullable = false, length = 36)
    private String workflowId;

    @Column(name = "node_id", nullable = false, length = 64)
    private String nodeId;

    @Column(name = "node_name", length = 128)
    private String nodeName;

    @Column(name = "source_table", length = 200)
    private String sourceTable;

    @Column(name = "target_table", length = 200)
    private String targetTable;

    @Column(name = "sql_type", length = 32)
    private String sqlType;

    /** Dialect used to parse this dependency (P2), e.g. MYSQL / CLICKHOUSE. */
    @Column(name = "dialect", length = 32)
    private String dialect;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}