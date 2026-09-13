package org.temporedata.modules.gov.security.controller;

import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.api.gov.security.AccessPolicy;
import org.temporedata.api.gov.security.GovernedResult;
import org.temporedata.common.util.Crypto;
import org.temporedata.modules.dev.workflow.runner.JdbcSupport;
import org.temporedata.modules.gov.security.AccessPolicyResolver;
import org.temporedata.modules.gov.security.DataAccessGovernor;
import org.temporedata.integration.core.datasource.entity.DatasourceEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * P1 auto-policy real execution entry: policy is derived automatically from enabled
 * {@code zy_mask_rule} (via {@link AccessPolicyResolver}) instead of being passed in.
 * Explicit datasource connection is used for POC; real wiring uses registered
 * datasources + secured endpoints.
 */
@RestController
@RequestMapping("/api/public/govern/auto")
public class AutoGovernedQueryController {

    private final JdbcSupport jdbcSupport;
    private final DataAccessGovernor governor;
    private final AccessPolicyResolver policyResolver;
    private final Crypto crypto;

    public AutoGovernedQueryController(JdbcSupport jdbcSupport,
                                       DataAccessGovernor governor,
                                       AccessPolicyResolver policyResolver,
                                       Crypto crypto) {
        this.jdbcSupport = jdbcSupport;
        this.governor = governor;
        this.policyResolver = policyResolver;
        this.crypto = crypto;
    }

    @PostMapping
    public BaseResponse<GovernedResult> query(@RequestBody Map<String, Object> body) {
        String sql = str(body.get("sql"));
        String datasourceId = str(body.get("datasourceId"));
        String tableName = str(body.get("tableName"));
        if (sql == null || sql.isBlank()) {
            throw new BusinessException("sql is required");
        }
        if (datasourceId == null || tableName == null) {
            throw new BusinessException("datasourceId/tableName are required for auto policy");
        }

        AccessPolicy policy = policyResolver.resolve(datasourceId, tableName);

        DatasourceEntity ds = DatasourceEntity.builder()
                .type(strOrNull(body.get("type"), "MYSQL"))
                .host(strOrNull(body.get("host"), "localhost"))
                .port(body.get("port") == null ? 3306 : Integer.parseInt(String.valueOf(body.get("port"))))
                .database(strOrNull(body.get("database"), "temporedata"))
                .username(strOrNull(body.get("username"), "root"))
                .password(crypto.encrypt(strOrNull(body.get("password"), "zy@2026")))
                .build();

        List<Map<String, Object>> rows = jdbcSupport.query(ds, sql);
        List<String> columns = rows.isEmpty() ? new ArrayList<>() : new ArrayList<>(rows.get(0).keySet());
        return BaseResponse.success(governor.govern(policy, columns, rows));
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private String strOrNull(Object o, String def) {
        String v = str(o);
        return (v == null || v.isBlank()) ? def : v;
    }
}