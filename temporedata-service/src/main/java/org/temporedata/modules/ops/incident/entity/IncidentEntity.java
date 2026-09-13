package org.temporedata.modules.ops.incident.entity;

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
 * Incident &amp; RCA event. Maps to temporedata_incident (V52).
 *
 * <p>[ENT-P2] Feeds the quality-issue -&gt; incident -&gt; RCA agent -&gt; action-plan loop.
 * datasetId is nullable (incidents may originate from job/API/infra, not a single dataset).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "temporedata_incident")
public class IncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 256)
    private String title;

    @Column(length = 32)
    private String sourceType;

    private Long datasetId;

    @Column(length = 16)
    private String severity;

    @Column(length = 32)
    private String status;

    @Lob
    private String rcaAnalysis;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}