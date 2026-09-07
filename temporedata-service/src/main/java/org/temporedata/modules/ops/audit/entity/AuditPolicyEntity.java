package org.temporedata.modules.ops.audit.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * Audit policy: which module/action is recorded and at what level (P3-12).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_policy")
public class AuditPolicyEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(length = 32)
    private String module;

    @Column(length = 64)
    private String action;

    @Column(name = "audit_level", length = 8)
    private String auditLevel; // NONE | SUMMARY | FULL

    @Column(name = "retention_days")
    private Integer retentionDays;

    @Column(name = "notify_alert")
    private Boolean notifyAlert;
}