package org.temporedata.api.dataset;

import lombok.Data;

/**
 * Unified data object (Dataset) contract shared across all Enterprise capability domains.
 *
 * <p>[ENT-P0] Top-level abstraction that metadata / modeling / quality / lineage / contract /
 * security all hang off. Optional transient fields below but set absent for the skeleton; the
 * owning service layer populates them.
 *
 * <p>Boundary contract: this is the cross-domain linkage key (mirrors V47__add_unified_dataset).
 */
@Data
public class DatasetDTO {

    private Long id;

    /** Display name. */
    private String name;

    /** Unique business code (e.g. dws_sales_daily). */
    private String code;

    /** DataDomain id this dataset belongs to. */
    private Long domainId;

    /** Data warehouse layer: ODS / DWD / DWS / ADS. */
    private String layer;

    /** Security classification level. */
    private String securityLevel;

    /** Data owner account. */
    private String owner;

    /** Audit timestamps (yyyy-MM-dd HH:mm:ss). */
    private String createdAt;

    private String updatedAt;
}