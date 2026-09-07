package org.temporedata.modules.gov.meta.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * Table-level metadata (design §3.1). Replaces the wide ad-hoc columns on zy_meta.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "temporedata_meta_table")
public class MetaTableEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "datasource_id", length = 64)
    private String datasourceId;

    @Column(name = "schema_name", length = 128)
    private String schemaName;

    @Column(name = "table_name", length = 200)
    private String tableName;

    @Column(name = "table_comment", length = 500)
    private String tableComment;

    @Column(name = "row_count")
    private Long rowCount;

    @Column(name = "data_size")
    private Long dataSize;

    @Column(name = "data_level_code", length = 32)
    private String dataLevelCode;

    @Column(name = "tags", length = 1000)
    private String tags;

    @Column(name = "status", length = 16)
    private String status;

    @Column(name = "last_sync_time", length = 32)
    private String lastSyncTime;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "lineage_count")
    private Integer lineageCount;
}