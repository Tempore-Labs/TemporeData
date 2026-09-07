package org.temporedata.modules.gov.security.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Third-party vulnerability tracker (P3-13).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vul_tracker")
public class VulnEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(length = 32)
    private String source; // SCA | SAST | MANUAL

    @Column(name = "package_name", length = 128)
    private String packageName;

    @Column(length = 64)
    private String cve;

    @Column(precision = 4, scale = 2)
    private BigDecimal cvss;

    @Column(length = 16)
    private String severity; // CRITICAL | HIGH | MEDIUM | LOW

    @Column(name = "affected_ver", length = 64)
    private String affectedVer;

    @Column(name = "fixed_ver", length = 64)
    private String fixedVer;

    @Column(length = 16)
    private String status; // OPEN | FIXING | FIXED | RISK_ACCEPTED

    @Column(name = "pr_ref", length = 128)
    private String prRef;

    @Column(length = 255)
    private String mitigation;

    @Column(name = "report_time", length = 32)
    private String reportTime;
}