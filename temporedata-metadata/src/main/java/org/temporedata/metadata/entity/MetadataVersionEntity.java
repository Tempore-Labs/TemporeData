package org.temporedata.metadata.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Lob;
import java.time.LocalDateTime;

/**
 * Metadata schema snapshot &amp; drift version. Maps to temporedata_metadata_version (V48).
 *
 * <p>[ENT-P0] One row per schema snapshot of a dataset; the latest row drives Schema
 * Drift detection (ADD_COLUMN / DROP_COLUMN / TYPE_CHANGE) for impact analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "temporedata_metadata_version")
public class MetadataVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long datasetId;

    @Column(nullable = false)
    private Integer version;

    @Lob
    @Column(nullable = false)
    private String schemaJson;

    private String driftType;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}