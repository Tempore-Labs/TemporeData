package org.temporedata.modules.ops.git.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * A bound Git repository (P2-10 Ops integration).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ops_repo")
public class OpsRepoEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "repo_ref", nullable = false, length = 200)
    private String repoRef;

    @Column(length = 64)
    private String branch;

    @Column(name = "auto_trigger", nullable = false)
    private Boolean autoTrigger;

    @Column(name = "deploy_script_path", length = 255)
    private String deployScriptPath;

    @Column(length = 32)
    private String environment;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "bind_info_json", columnDefinition = "TEXT")
    private String bindInfoJson;

    @Column(name = "create_time", length = 32)
    private String createTime;

    @Column(name = "update_time", length = 32)
    private String updateTime;
}