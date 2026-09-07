package org.temporedata.api.gov.quality;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Rich quality rule execution result (P1-6 real execution).
 */
@Data
public class QualityRunRes {

    private String checkId;
    private String ruleId;
    private String ruleName;
    /** PASS | FAIL. */
    private String status;
    /** Pass rate 0..1 (or null when not measurable). */
    private Double passRate;
    private Long totalRows;
    private Long badRows;
    private List<String> badSample = new ArrayList<>();
    private String result;
    private long durationMs;
    private String errorMsg;
    private String checkTime;
}