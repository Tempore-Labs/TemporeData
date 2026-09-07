package org.temporedata.modules.asset.mydata.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Mydata entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_mydata")
public class MydataEntity {

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
    private String userId;
    @Column
    private String resourceType;
    @Column
    private String resourceId;
    @Column
    private String resourceName;
    @Column
    private String expireTime;
    @Column
    private String accessType;
    @Column
    private String grantedBy;
    @Column
    private String createTime;
}
