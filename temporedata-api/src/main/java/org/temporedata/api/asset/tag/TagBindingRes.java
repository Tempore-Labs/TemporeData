package org.temporedata.api.asset.tag;

import lombok.Data;

/**
 * Tag-asset binding DTO with tag info.
 */
@Data
public class TagBindingRes {

    private String tagId;

    private String tagName;

    private String tagColor;

    private String assetType;

    private String assetId;

    private String assetName;

    private String createTime;
}