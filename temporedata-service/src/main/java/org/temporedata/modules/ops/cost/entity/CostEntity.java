package org.temporedata.modules.ops.cost.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FinOps cost ledger. Maps to temporedata_data_cost (V52).
 *
 * <p>[ENT-P2] One aggregate row per dataset per day; feeds compute/storage/query cost
 * dashboards and cost-reduction advice in the temporedata-service cost domain.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "temporedata_data_cost")
public class CostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long datasetId;

    @Column(precision = 10, scale = 2)
    private BigDecimal computeCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal storageCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal queryCost;

    @Column(nullable = false)
    private LocalDate recordDate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Total cost convenience. */
    public BigDecimal total() {
        BigDecimal t = BigDecimal.ZERO;
        if (computeCost != null) t = t.add(computeCost);
        if (storageCost != null) t = t.add(storageCost);
        if (queryCost != null) t = t.add(queryCost);
        return t;
    }
}