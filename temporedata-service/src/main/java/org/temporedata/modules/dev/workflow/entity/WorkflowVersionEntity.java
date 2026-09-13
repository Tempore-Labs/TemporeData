package org.temporedata.modules.dev.workflow.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.LocalDateTime;

/** Immutable snapshot of a workflow definition at a point in time. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zy_wf_version")
public class WorkflowVersionEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "workflow_id", length = 36)
    private String workflowId;

    @Column(name = "version_no")
    private Integer versionNo;

    @Column
    private String name;

    @Lob
    @Column(name = "nodes_json", columnDefinition = "LONGTEXT")
    private String nodesJson;

    @Lob
    @Column(name = "edges_json", columnDefinition = "LONGTEXT")
    private String edgesJson;

    @Column
    private String remark;

    @Column(name = "create_by", length = 32)
    private String createBy;

    @CreationTimestamp
    @Column(name = "create_date_time")
    private LocalDateTime createDateTime;

    @Column(name = "create_time", length = 32)
    private String createTime;
}