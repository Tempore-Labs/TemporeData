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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Quality gate config. Maps to temporedata_quality_gate (V49).
 *
 * <p>[ENT-P0] Couples with the WorkflowEngine: when the evaluated score drops below
 * {@code minScore} the gate is set to BLOCKED and downstream publishing is held.
 * One active gate per dataset+rule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "temporedata_quality_gate")
public class QualityGateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long datasetId;

    private Long ruleId;

    @Column(precision = 5, scale = 2)
    private BigDecimal minScore;

    @Column(length = 32)
    private String status;

    @Column
    private Boolean blocking;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}