package org.temporedata.asset.entity;

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
import java.time.LocalDateTime;

/**
 * Unified asset catalog entry. Maps to temporedata_asset_catalog (V50).
 *
 * <p>[ENT-P0] One catalog row per dataset (dataset_id unique); binds business terms, tags,
 * popularity and owner for the asset marketplace &amp; lineage embed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "temporedata_asset_catalog")
public class AssetCatalogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long datasetId;

    @Column(length = 256)
    private String businessTerm;

    @Column(length = 512)
    private String tags;

    private Integer popularity;

    @Column(length = 64)
    private String owner;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Asset-detail projection returned by the asset API. */
    public interface AssetView {
        Long getDatasetId();
        String getBusinessTerm();
        String getTags();
        Integer getPopularity();
        String getOwner();
    }
}