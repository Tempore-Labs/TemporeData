package org.temporedata.modules.ops.git.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * A build / deploy triggered via Ops (P2-10).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ops_build")
public class OpsBuildEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "repo_id", nullable = false, length = 36)
    private String repoId;

    @Column(name = "trigger_type", length = 16)
    private String triggerType; // COMMIT | PR | MANUAL | WEBHOOK

    @Column(name = "ref_name", length = 64)
    private String refName;

    @Column(name = "commit_sha", length = 64)
    private String commitSha;

    @Column(name = "commit_message", length = 500)
    private String commitMessage;

    @Column(nullable = false, length = 16)
    private String status; // TRIGGERED | SUCCESS | FAILED

    @Column(name = "pipeline_ref", length = 128)
    private String pipelineRef;

    @Column(name = "artifact_id", length = 36)
    private String artifactId;

    @Column(name = "log_ref", length = 64)
    private String logRef;

    @Column(name = "create_time", length = 32)
    private String createTime;

    @Column(name = "update_time", length = 32)
    private String updateTime;
}