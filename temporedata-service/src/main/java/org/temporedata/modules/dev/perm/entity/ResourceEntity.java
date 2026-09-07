package org.temporedata.modules.dev.perm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * Resource catalog entry (P3-11).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "res_resource")
public class ResourceEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "resource_type", nullable = false, length = 32)
    private String resourceType;

    @Column(name = "resource_key", nullable = false, length = 128)
    private String resourceKey;

    @Column(name = "resource_name", length = 128)
    private String resourceName;

    @Column(name = "tenant_id", length = 32)
    private String tenantId;

    @Column(length = 32)
    private String owner;

    @Column(length = 255)
    private String description;

    @Column(name = "create_time", length = 32)
    private String createTime;
}