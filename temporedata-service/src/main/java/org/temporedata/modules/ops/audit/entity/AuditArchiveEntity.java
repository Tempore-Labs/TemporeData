package org.temporedata.modules.ops.audit.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * Periodical audit archive with a signed root hash (P3-12).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_archive")
public class AuditArchiveEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "period_start", length = 32)
    private String periodStart;

    @Column(name = "period_end", length = 32)
    private String periodEnd;

    @Column(name = "record_count")
    private Integer recordCount;

    @Column(name = "root_hash", length = 64)
    private String rootHash;

    @Column(name = "signed_by", length = 64)
    private String signedBy;

    @Column(name = "archive_path", length = 255)
    private String archivePath;

    @Column(name = "create_time", length = 32)
    private String createTime;
}