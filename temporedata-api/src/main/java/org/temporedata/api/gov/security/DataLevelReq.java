package org.temporedata.api.gov.security;

import lombok.Data;

/**
 * Data sensitivity level request.
 */
@Data
public class DataLevelReq {

    private String name;

    private String code; // PUBLIC, INTERNAL, CONFIDENTIAL, SECRET

    private String description;

    private Integer sortOrder;
}