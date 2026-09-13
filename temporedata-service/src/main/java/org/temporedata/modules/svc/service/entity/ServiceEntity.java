package org.temporedata.modules.svc.service.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Service entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_service")
public class ServiceEntity {

    @Id @GeneratedValue(generator = "uuid2") @GenericGenerator(name = "uuid2", strategy = "uuid2") @Column(length = 36)
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
    private String name;
    @Column
    private String description;
    @Column
    private String datasourceId;
    @Column
    private String sql;
    @Column
    private String method;
    @Column
    private String path;
    @Column
    private String cacheTtl;
    @Column
    private String apiKey;
    @Column
    private String status;
    @Column
    private Long callCount;
    @Column
    private String lastCallTime;
    @Column
    private String createTime;
    @Column
    private String updateTime;
}
