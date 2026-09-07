package org.temporedata.modules.gov.meta.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/** Metadata sync session log (P1, design §3.3). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zy_meta_sync_log")
public class MetaSyncLogEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "datasource_id", length = 64)
    private String datasourceId;

    @Column(name = "sync_type", length = 16)
    private String syncType;

    @Column(name = "status", length = 16)
    private String status;

    @Column(name = "started_at", length = 32)
    private String startedAt;

    @Column(name = "finished_at", length = 32)
    private String finishedAt;

    @Column(name = "tables_discovered")
    private Integer tablesDiscovered;

    @Column(name = "columns_discovered")
    private Integer columnsDiscovered;

    @Column(name = "unchanged_tables")
    private Integer unchangedTables;

    @Column(name = "errors", length = 2000)
    private String errors;
}