package org.temporedata.api.asset.tag;

import lombok.Data;

import java.util.List;

/**
 * Bind/unbind tags to a data asset.
 */
@Data
public class TagBindingReq {

    private String assetType;

    private String assetId;

    private String assetName;

    private List<String> tagIds;
}