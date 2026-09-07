package org.temporedata.api.integration.secret;

import lombok.Data;

/**
 * Secret list / detail response.
 */
@Data
public class SecretRes {

    private String id;

    private String key;

    private String value;

    private String description;

    private String scope;

    private String createTime;
}