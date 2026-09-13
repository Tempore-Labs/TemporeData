package org.temporedata.contract.entity;

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
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Declarative data contract. Maps to temporedata_data_contract (V51).
 *
 * <p>[ENT-P1] One contract per dataset (dataset_id unique); status drives the producer gate:
 * DRAFT -&gt; ACTIVE (may publish) -&gt; BROKEN (publish blocked) / SUPERSEDED (new revision).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "temporedata_data_contract")
public class ContractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long datasetId;

    @Lob
    @Column(nullable = false)
    private String contractYaml;

    @Column(length = 32)
    private String compatibilityMode;

    @Column(length = 32)
    private String status;

    @Column(length = 64)
    private String owner;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}