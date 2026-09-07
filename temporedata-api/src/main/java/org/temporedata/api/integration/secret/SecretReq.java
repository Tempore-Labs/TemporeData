package org.temporedata.api.integration.secret;

import lombok.Data;

/**
 * Create / update secret request.
 */
@Data
public class SecretReq {

    private String key;

    private String value;

    private String description;

    private String scope; // GLOBAL, WORKFLOW
}