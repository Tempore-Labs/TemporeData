package org.temporedata.modules.ops.cluster.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Cluster entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_cluster")
public class ClusterEntity {

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
    private String host;
    @Column
    private Integer port;
    @Column
    private String username;
    @Column
    private String password;
    @Column
    private String agentStatus;
    @Column
    private Double cpuUsage;
    @Column
    private Double memoryUsage;
    @Column
    private Double diskUsage;
    @Column
    private String name;
    @Column
    private String type;
    @Column
    private String masterUrl;
    @Column
    private String status;
    @Column
    private Integer nodeCount;
    @Column
    private String createTime;
}
