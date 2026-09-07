package org.temporedata.api.gov.security;

import lombok.Data;

import java.util.List;

/**
 * Data category response (tree structure).
 */
@Data
public class DataCategoryRes {

    private String id;

    private String name;

    private String code;

    private String parentId;

    private String levelName;

    private String levelCode;

    private String description;

    private String createTime;

    private List<DataCategoryRes> children;
}