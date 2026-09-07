package org.temporedata.modules.asset.permapproval.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Permapproval entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_permapproval")
public class PermapprovalEntity {

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
    private String resourceType;
    @Column
    private String resourceId;
    @Column
    private String resourceName;
    @Column
    private String accessType;
    @Column
    private String reason;
    @Column
    private String expireTime;
    @Column
    private String applicantId;
    @Column
    private String applicantName;
    @Column
    private String status;
    @Column
    private String approverId;
    @Column
    private String approverName;
    @Column
    private String approveComment;
    @Column
    private String createTime;
    @Column
    private String updateTime;
}
