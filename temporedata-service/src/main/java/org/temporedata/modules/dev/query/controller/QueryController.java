package org.temporedata.modules.dev.query.controller;

import org.temporedata.modules.dev.query.entity.QueryEntity;
import org.temporedata.modules.dev.query.service.QueryService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    /**
     * Execute a SQL query.
     * POST /api/query/execute
     * Request body: { "sql": "...", "datasourceId": "...", "datasourceName": "..." }
     */
    @PostMapping("/execute")
    public BaseResponse<QueryEntity> execute(@RequestBody Map<String, String> request) {
        String sql = request.get("sql");
        String datasourceId = request.get("datasourceId");
        String datasourceName = request.get("datasourceName");
        return BaseResponse.success(queryService.execute(sql, datasourceId, datasourceName));
    }

    /**
     * Query execution history.
     * GET /api/query/history?page=0&size=20
     */
    @GetMapping("/history")
    public BaseResponse<Page<QueryEntity>> history(Pageable pageable) {
        return BaseResponse.success(queryService.history(pageable));
    }
}