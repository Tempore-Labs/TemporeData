package org.temporedata.modules.svc.service.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** DataApi entity for exposing custom SQL endpoints. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_data_api")
public class DataApiEntity {

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

    @Column(length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 32)
    private String datasourceId;

    @Column(name = "`sql`", length = 1000)
    private String sql;

    @Column(length = 8)
    private String method;

    @Column(length = 256)
    private String path;

    @Column(length = 16)
    private String status;

    @Column(length = 64)
    private String apiKey;

    @Column(length = 32)
    private String createTime;

    @Column(length = 32)
    private String updateTime;
}