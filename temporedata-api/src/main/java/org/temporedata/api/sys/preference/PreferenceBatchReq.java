package org.temporedata.api.sys.preference;

import lombok.Data;

import java.util.Map;

/**
 * Batch save preferences request.
 */
@Data
public class PreferenceBatchReq {

    private Map<String, String> preferences;
}