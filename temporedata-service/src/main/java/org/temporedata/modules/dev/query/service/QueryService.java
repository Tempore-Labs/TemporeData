package org.temporedata.modules.dev.query.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.temporedata.api.gov.security.GovExecResult;
import org.temporedata.modules.dev.query.entity.QueryEntity;
import org.temporedata.modules.dev.query.repository.QueryRepository;
import org.temporedata.modules.gov.security.GovernedSqlExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService {

    private final QueryRepository queryRepository;
    private final GovernedSqlExecutor governedSqlExecutor;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Paginated query history.
     */
    public Page<QueryEntity> history(Pageable pageable) {
        return queryRepository.findAll(pageable);
    }

    /**
     * Execute a real SQL SELECT against the datasource through the data-access
     * governance chain (数据访问控制与动态脱敏执行链 v1.0): column pruning + dynamic masking,
     * pushed to the data source when possible (Stage B), else applied on the result
     * (Stage A). Output is persisted as JSON for history and returned for rendering.
     */
    @Transactional
    public QueryEntity execute(String sql, String datasourceId, String datasourceName) {
        String now = LocalDateTime.now().format(DTF);

        QueryEntity entity = QueryEntity.builder()
                .sql(sql)
                .datasourceId(datasourceId)
                .datasourceName(datasourceName)
                .createTime(now)
                .build();

        try {
            GovExecResult res = governedSqlExecutor.execute(sql, datasourceId, "QUERY");

            entity.setStatus("SUCCESS");
            entity.setRowCount(res.getRowCount());
            entity.setDurationMs(res.getDurationMs());
            entity.setErrorMsg(null);
            entity.setColumns(res.getColumns());
            entity.setRows(res.getRows());
            entity.setColumnsJson(toJson(res.getColumns()));
            entity.setRowsJson(toJson(res.getRows()));

            log.info("Query executed (governed): ds={}, table={}, rows={}, columns={}, maskCols={}, pushdown={}",
                    datasourceId, res.getTable(), res.getRowCount(), res.getColumns().size(),
                    res.getMaskCols(), res.isPushed());
        } catch (Exception e) {
            log.error("Query execution FAILED: ds={}, sql={}", datasourceId, sql, e);
            entity.setStatus("FAILED");
            entity.setRowCount(0);
            entity.setDurationMs(0);
            entity.setErrorMsg(e.getMessage());
        }

        return queryRepository.save(entity);
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return null;
        }
    }
}