package org.temporedata.modules.integration.security.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * Desensitization rule persisted in zy_mask_rule (P1). Binds a masking function to a
 * column of a datasource/table; enabled rules (<code>status=1</code>) drive the
 * {@code AccessPolicyResolver}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zy_mask_rule")
public class MaskRuleEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "rule_type", nullable = false, length = 32)
    private String ruleType;

    @Column(name = "mask_pattern", length = 200)
    private String maskPattern;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "datasource_id", length = 64)
    private String datasourceId;

    @Column(name = "table_name", length = 200)
    private String tableName;

    @Column(name = "column_name", length = 200)
    private String columnName;

    @Column(name = "status")
    private Integer status;
}