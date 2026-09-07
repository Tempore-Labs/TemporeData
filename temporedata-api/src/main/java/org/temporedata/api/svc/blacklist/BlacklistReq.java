package org.temporedata.api.svc.blacklist;

import lombok.Data;

/**
 * Blacklist / whitelist request DTO.
 */
@Data
public class BlacklistReq {

    private String ipAddress;

    private String listType;

    private String reason;
}