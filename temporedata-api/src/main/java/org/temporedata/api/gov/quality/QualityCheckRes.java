package org.temporedata.api.gov.quality;

import lombok.Data;

/**
 * Quality check execution result.
 */
@Data
public class QualityCheckRes {

    private String checkId;

    private String ruleName;

    private String status; // PASS, FAIL

    private String result; // readable summary

    private long durationMs;

    private String errorMsg;

    private String checkTime;
}