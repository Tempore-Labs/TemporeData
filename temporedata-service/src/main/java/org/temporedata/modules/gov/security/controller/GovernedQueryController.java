package org.temporedata.modules.gov.security.controller;

import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.api.gov.security.AccessPolicy;
import org.temporedata.api.gov.security.GovernedResult;
import org.temporedata.common.util.Crypto;
import org.temporedata.modules.dev.workflow.runner.JdbcSupport;
import org.temporedata.modules.gov.security.DataAccessGovernor;
import org.temporedata.modules.integration.datasource.entity.DatasourceEntity;
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
 * Real end-to-end test entry for the DataAccessGovernor: provides an explicit
 * datasource connection (host/port/db/user/password) + a SELECT SQL + an AccessPolicy,
 * executes against the database, then applies column pruning + desensitization.
 * Route under /api/public/** is POC only; real wiring should use registered datasources
 * and secured endpoints in P1+.
 */
@RestController
@RequestMapping("/api/public/govern/query")
public class GovernedQueryController {

    private final JdbcSupport jdbcSupport;
    private final DataAccessGovernor governor;
    private final Crypto crypto;

    public GovernedQueryController(JdbcSupport jdbcSupport, DataAccessGovernor governor, Crypto crypto) {
        this.jdbcSupport = jdbcSupport;
        this.governor = governor;
        this.crypto = crypto;
    }

    @PostMapping
    public BaseResponse<GovernedResult> query(@RequestBody Map<String, Object> body) {
        String sql = str(body.get("sql"));
        if (sql == null || sql.isBlank()) {
            throw new BusinessException("sql is required");
        }
        DatasourceEntity ds = DatasourceEntity.builder()
                .type(strOrNull(body.get("type"), "MYSQL"))
                .host(strOrNull(body.get("host"), "localhost"))
                .port(body.get("port") == null ? 3306 : Integer.parseInt(String.valueOf(body.get("port"))))
                .database(str(body.get("database")))
                .username(str(body.get("username")))
                .password(crypto.encrypt(str(body.get("password"))))
                .build();

        List<Map<String, Object>> rows = jdbcSupport.query(ds, sql);
        List<String> columns = rows.isEmpty() ? new ArrayList<>() : new ArrayList<>(rows.get(0).keySet());
        AccessPolicy policy = toPolicy((Map<String, Object>) body.get("policy"));
        return BaseResponse.success(governor.govern(policy, columns, rows));
    }

    private AccessPolicy toPolicy(Map<String, Object> p) {
        AccessPolicy policy = new AccessPolicy();
        if (p == null) {
            return policy;
        }
        if (Boolean.TRUE.equals(p.get("denied"))) {
            policy.setDenied(true);
        }
        List<String> granted = strList(p.get("grantedColumns"));
        if (!granted.isEmpty()) {
            policy.setGrantedColumns(new LinkedHashSet<>(granted));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> rules = (Map<String, Object>) p.get("desensitizeRules");
        if (rules != null) {
            LinkedHashMap<String, String> m = new LinkedHashMap<>();
            rules.forEach((k, v) -> m.put(k, String.valueOf(v)));
            policy.setDesensitizeRules(m);
        }
        policy.setRowFilter(p.get("rowFilter") == null ? null : String.valueOf(p.get("rowFilter")));
        return policy;
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private String strOrNull(Object o, String def) {
        String v = str(o);
        return (v == null || v.isBlank()) ? def : v;
    }

    private List<String> strList(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List) {
            for (Object e : (List<?>) o) {
                out.add(String.valueOf(e));
            }
        }
        return out;
    }
}