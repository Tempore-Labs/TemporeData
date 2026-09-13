package org.temporedata.modules.asset.tag.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Tag entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_tag")
public class TagEntity {

    @Id @GeneratedValue(generator = "uuid2") @GenericGenerator(name = "uuid2", strategy = "uuid2") @Column(length = 36)
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
    private String assetType;
    @Column
    private String assetId;
    @Column
    private String assetName;
    @Column
    private String tagId;
    @Column
    private String tagName;
    @Column
    private String tagColor;
    @Column
    private String createTime;
    @Column
    private String name;
    @Column
    private String code;
    @Column
    private String description;
    @Column
    private String color;
    @Column
    private String classificationId;
    @Column
    private Boolean isMutuallyExclusive;
    @Column
    private String status;
    @Column
    private String derivedFromTagId;
    @Column
    private String definition;
    @Column
    private String synonyms;
    @Column
    private Boolean isTerm;
    @Column
    private String classificationName;
    @Column
    private String updateTime;
}
