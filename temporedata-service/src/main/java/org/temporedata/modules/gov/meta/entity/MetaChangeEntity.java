package org.temporedata.modules.gov.meta.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/** Table-structure change event (P3, design §6). Source event for downstream linkage. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zy_meta_change")
public class MetaChangeEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "datasource_id", length = 64)
    private String datasourceId;

    @Column(name = "table_name", length = 200)
    private String tableName;

    @Column(name = "change_type", length = 16)
    private String changeType;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "synced_at", length = 32)
    private String syncedAt;
}