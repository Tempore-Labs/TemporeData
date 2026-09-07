package org.temporedata.modules.gov.catalog.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Catalog entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_catalog")
public class CatalogEntity {

    @Id @GeneratedValue(generator = "uuid2") @GenericGenerator(name = "uuid2", strategy = "uuid2") @Column(length = 32)
    private String id;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createDateTime;

    @Column(length = 32)
    private String createBy;

    @UpdateTimestamp
    private LocalDateTime updateDateTime;

    @Column(length = 32)
    private String updateBy;

    @Column(length = 32)
    private String tenantId;
    @Column
    private String label;
    @Column
    private String type;
    @Column
    private String datasourceName;
    @Column
    private String schemaName;
    @Column
    private String tableName;
    @Column
    private Long rowCount;
    @Column
    private String comment;
    @Column
    private Boolean isLeaf;
    @Column
    private String columnName;
    @Column
    private String dataType;
    @Column
    private Integer columnSize;
    @Column
    private Integer decimalDigits;
    @Column
    private String defaultValue;
    @Column
    private Integer ordinalPosition;
    @Column
    private Boolean isNullable;
    @Column
    private Boolean isPrimaryKey;
    @Column
    private String dataLevelId;
    @Column
    private String tableId;
    @Column
    private String columnId;
    @Column
    private String dataCategoryId;
    @Column
    private String relationType;
    @Column
    private String relationName;
    @Column
    private String datasourceId;
    @Column
    private String syncTime;
}
