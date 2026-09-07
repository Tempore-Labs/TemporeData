package org.temporedata.modules.ops.audit.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * Unified audit event with tamper-evident hash chain (P3-12).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_event")
public class AuditEventEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "event_time", length = 32)
    private String eventTime;

    @Column(name = "tenant_id", length = 32)
    private String tenantId;

    @Column(length = 32)
    private String operator;

    @Column(length = 64)
    private String ip;

    @Column(length = 64)
    private String action;

    @Column(name = "resource_type", length = 32)
    private String resourceType;

    @Column(name = "resource_key", length = 128)
    private String resourceKey;

    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;

    @Column(length = 32)
    private String module;

    @Column(name = "req_id", length = 64)
    private String reqId;

    @Column(length = 16)
    private String status;

    @Column
    private Long seq;

    @Column(name = "prev_hash", length = 64)
    private String prevHash;

    @Column(name = "event_hash", length = 64)
    private String eventHash;
}