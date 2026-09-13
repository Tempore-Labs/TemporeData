package org.temporedata.quality.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Lob;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Quality rule definition. Maps to temporedata_quality_rule (V49).
 *
 * <p>[ENT-P0] One validation rule (Completeness / Uniqueness / Validity / Accuracy /
 * Timeliness) per dataset; {@code isBlocking} drives the Quality Gate to block downstream.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "temporedata_quality_rule")
public class QualityRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long datasetId;

    @Column(length = 32)
    private String dimension;

    @Column(length = 64)
    private String ruleType;

    @Lob
    private String expression;

    @Column(precision = 5, scale = 2)
    private BigDecimal thresholdScore;

    @Column(name = "is_blocking")
    private Boolean isBlocking;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}