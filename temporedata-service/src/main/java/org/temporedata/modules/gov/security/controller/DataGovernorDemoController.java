package org.temporedata.modules.gov.security.controller;

import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.api.gov.security.AccessPolicy;
import org.temporedata.api.gov.security.GovernedResult;
import org.temporedata.modules.gov.security.DataAccessGovernor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * POC endpoint to demonstrate the DataAccessGovernor (列裁剪 + 流式脱敏) without a
 * real datasource. Body carries the raw columns/rows plus an AccessPolicy; the
 * governed result is returned. Route is under /api/public/** for demo (real
 * access decision wiring lives in P1+ under secured /api/security/**).
 */
@RestController
@RequestMapping("/api/public/govern")
public class DataGovernorDemoController {

    private final DataAccessGovernor governor;

    public DataGovernorDemoController(DataAccessGovernor governor) {
        this.governor = governor;
    }

    @PostMapping
    public BaseResponse<GovernedResult> govern(@RequestBody Map<String, Object> body) {
        List<String> columns = toStringList(body.get("columns"));
        List<Map<String, Object>> rows = toRowList(body.get("rows"));
        AccessPolicy policy = toPolicy((Map<String, Object>) body.get("policy"));
        return BaseResponse.success(governor.govern(policy, columns, rows));
    }

    @SuppressWarnings("unchecked")
    private AccessPolicy toPolicy(Map<String, Object> p) {
        AccessPolicy policy = new AccessPolicy();
        if (p == null) {
            return policy;
        }
        if (Boolean.TRUE.equals(p.get("denied"))) {
            policy.setDenied(true);
        }
        List<String> granted = toStringList(p.get("grantedColumns"));
        if (!granted.isEmpty()) {
            policy.setGrantedColumns(new LinkedHashSet<>(granted));
        }
        Map<String, Object> rules = (Map<String, Object>) p.get("desensitizeRules");
        if (rules != null) {
            LinkedHashMap<String, String> m = new LinkedHashMap<>();
            rules.forEach((k, v) -> m.put(k, String.valueOf(v)));
            policy.setDesensitizeRules(m);
        }
        Map<String, Object> patterns = (Map<String, Object>) p.get("maskPatterns");
        if (patterns != null) {
            LinkedHashMap<String, String> m = new LinkedHashMap<>();
            patterns.forEach((k, v) -> m.put(k, String.valueOf(v)));
            policy.setMaskPatterns(m);
        }
        policy.setRowFilter(p.get("rowFilter") == null ? null : String.valueOf(p.get("rowFilter")));
        return policy;
    }

    private List<String> toStringList(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List) {
            for (Object e : (List<?>) o) {
                out.add(String.valueOf(e));
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toRowList(Object o) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (o instanceof List) {
            for (Object e : (List<?>) o) {
                if (e instanceof Map) {
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                    ((Map<String, Object>) e).forEach(row::put);
                    out.add(row);
                }
            }
        }
        return out;
    }
}