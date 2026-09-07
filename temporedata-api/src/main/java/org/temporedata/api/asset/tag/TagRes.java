package org.temporedata.api.asset.tag;

import lombok.Data;

/**
 * Tag list / detail response.
 */
@Data
public class TagRes {

    private String id;

    private String name;

    private String code;

    private String description;

    private String color;

    private String classificationId;

    private String classificationName;

    private Boolean isMutuallyExclusive;

    private String status;

    private String derivedFromTagId;

    private String definition;

    private String synonyms;

    private Boolean isTerm;

    private String tenantId;

    private String createTime;

    private String updateTime;
}