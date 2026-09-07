package org.temporedata.api.sys.preference;

import lombok.Data;

/**
 * Save single preference request.
 */
@Data
public class PreferenceReq {

    private String prefKey;

    private String prefValue;
}