package org.temporedata.api.svc.form;

import lombok.Data;

/**
 * Create / update form request.
 */
@Data
public class FormReq {

    private String name;

    private String description;

    private String config; // JSON form schema
}