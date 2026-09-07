package org.temporedata.modules.gov.security.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Security entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_security")
public class SecurityEntity {

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
    private String name;
    @Column
    private String code;
    @Column
    private String parentId;
    @Column
    private String levelId;
    @Column
    private String description;
    @Column
    private String levelName;
    @Column
    private String levelCode;
    @Column
    private String createTime;
    @Column
    private Integer sortOrder;
    @Column
    private String ruleType;
    @Column
    private String maskPattern;
    @Column
    private String datasourceId;
    @Column
    private String tableName;
    @Column
    private String columnName;
    @Column
    private Integer status;
    @Column
    private String updateTime;
}
