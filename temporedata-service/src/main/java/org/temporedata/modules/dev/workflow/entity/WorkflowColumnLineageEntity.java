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
 * Per-node column-level lineage (P3, best-effort). A target column produced
 * from a source column of another table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "temporedata_column_lineage")
public class WorkflowColumnLineageEntity {

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

    @Column(name = "target_table", length = 200)
    private String targetTable;

    @Column(name = "target_column", length = 128)
    private String targetColumn;

    @Column(name = "source_table", length = 200)
    private String sourceTable;

    @Column(name = "source_column", length = 128)
    private String sourceColumn;

    @Column(name = "dialect", length = 32)
    private String dialect;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}