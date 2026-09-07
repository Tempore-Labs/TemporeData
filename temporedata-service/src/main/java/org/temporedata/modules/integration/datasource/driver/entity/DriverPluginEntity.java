package org.temporedata.modules.integration.datasource.driver.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*; import java.time.LocalDateTime;

/**
 * Uploaded datasource plugin record (maps zy_datasource_plugin). Tracks an
 * external JDBC driver jar that is isolated-loaded by PluginManager.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_datasource_plugin")
public class DriverPluginEntity {

    @Id @GeneratedValue(generator = "uuid2") @GenericGenerator(name = "uuid2", strategy = "uuid2") @Column(length = 64)
    private String id;

    @Column(name = "db_type", length = 32)
    private String dbType;

    @Column(length = 100)
    private String name;

    @Column(length = 50)
    private String version;

    @Column(name = "driver_class", length = 255)
    private String driverClass;

    @Column(name = "driver_path", length = 500)
    private String driverPath;

    @Column(name = "mvn_group", length = 128)
    private String mvnGroup;

    @Column(name = "mvn_artifact", length = 128)
    private String mvnArtifact;

    @Column(name = "mvn_version", length = 50)
    private String mvnVersion;

    @Column(length = 20)
    private String status;

    @Column
    private Boolean builtin;

    @Column(name = "tenant_id", length = 32)
    private String tenantId;

    @CreationTimestamp @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}