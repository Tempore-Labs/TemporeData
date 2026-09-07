package org.temporedata.modules.gov.security.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * QA report record bound to a version (P3-13).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "qa_report")
public class QaReportEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(length = 32)
    private String version;

    @Column(length = 64)
    private String module;

    @Column(length = 32)
    private String kind; // UNIT | INTEGRATION | API | COVERAGE

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(precision = 5, scale = 2)
    private BigDecimal coverage;

    @Column(length = 16)
    private String gate; // PASS | FAIL | BLOCK

    @Column(name = "run_time", length = 32)
    private String runTime;
}