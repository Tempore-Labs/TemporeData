package org.temporedata.modules.gov.meta.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * Column-level metadata (design §3.2), linked to a table row via tableId.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zy_meta_column")
public class MetaColumnEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "table_id", length = 36)
    private String tableId;

    @Column(name = "datasource_id", length = 64)
    private String datasourceId;

    @Column(name = "table_name", length = 200)
    private String tableName;

    @Column(name = "column_name", length = 200)
    private String columnName;

    @Column(name = "column_type", length = 64)
    private String columnType;

    @Column(name = "column_size")
    private Integer columnSize;

    @Column(name = "nullable")
    private Boolean nullable;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "primary_key")
    private Boolean primaryKey;

    @Column(name = "ordinal_position")
    private Integer ordinalPosition;

    @Column(name = "sensitive_flag")
    private Boolean sensitiveFlag;

    @Column(name = "data_level_code", length = 32)
    private String dataLevelCode;

    @Column(name = "status", length = 16)
    private String status;
}