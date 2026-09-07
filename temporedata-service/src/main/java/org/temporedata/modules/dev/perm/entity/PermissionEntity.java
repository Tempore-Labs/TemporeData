package org.temporedata.modules.dev.perm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * Role-resource permission entry (P3-11).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "res_permission")
public class PermissionEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "role_id", nullable = false, length = 36)
    private String roleId;

    @Column(name = "resource_type", nullable = false, length = 32)
    private String resourceType;

    @Column(name = "resource_key", length = 128)
    private String resourceKey; // '*' = all

    @Column(nullable = false, length = 16)
    private String action; // READ | WRITE | EXECUTE | ADMIN

    @Column(nullable = false, length = 8)
    private String scope; // ALLOW | DENY

    @Column(name = "tenant_id", length = 32)
    private String tenantId;

    @Column(name = "create_time", length = 32)
    private String createTime;
}