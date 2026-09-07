package org.temporedata.api.gov.security;

import lombok.Data;

/**
 * Data category request.
 */
@Data
public class DataCategoryReq {

    private String name;

    private String code;

    private String parentId; // null for root category

    private String levelId; // reference to DataLevel

    private String description;
}