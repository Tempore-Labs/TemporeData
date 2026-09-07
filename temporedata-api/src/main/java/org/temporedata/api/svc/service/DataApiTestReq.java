package org.temporedata.api.svc.service;

import lombok.Data;

import java.util.Map;

/**
 * Data API test request with parameters.
 */
@Data
public class DataApiTestReq {

    private Map<String, Object> params; // SQL parameter bindings
}