package org.temporedata.modules.gov.meta.controller;

import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.modules.gov.meta.entity.MetaChangeEntity;
import org.temporedata.modules.gov.meta.entity.MetaColumnEntity;
import org.temporedata.modules.gov.meta.entity.MetaTableEntity;
import org.temporedata.modules.gov.meta.repository.MetaChangeRepository;
import org.temporedata.modules.gov.meta.repository.MetaColumnRepository;
import org.temporedata.modules.gov.meta.repository.MetaTableRepository;
import org.temporedata.modules.gov.meta.service.MetaCollector;
import org.temporedata.modules.gov.meta.service.MetaCoordinator;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Meta P0 endpoints: real JDBC metadata collection (explicit datasource connection,
 * POC for verification; real wiring uses registered datasources) + layered query.
 */
@RestController
@RequestMapping("/api/public/meta")
public class MetaCollectController {

    private final MetaCollector collector;
    private final MetaCoordinator coordinator;
    private final MetaTableRepository tableRepo;
    private final MetaColumnRepository columnRepo;
    private final MetaChangeRepository changeRepo;

    public MetaCollectController(MetaCollector collector,
                                 MetaCoordinator coordinator,
                                 MetaTableRepository tableRepo,
                                 MetaColumnRepository columnRepo,
                                 MetaChangeRepository changeRepo) {
        this.collector = collector;
        this.coordinator = coordinator;
        this.tableRepo = tableRepo;
        this.columnRepo = columnRepo;
        this.changeRepo = changeRepo;
    }

    @PostMapping("/collect")
    public BaseResponse<Map<String, Object>> collect(@RequestBody Map<String, Object> body) {
        String host = strOrNull(body.get("host"), "localhost");
        int port = body.get("port") == null ? 3306 : Integer.parseInt(String.valueOf(body.get("port")));
        String database = str(body.get("database"));
        String username = strOrNull(body.get("username"), "root");
        String password = strOrNull(body.get("password"), "");
        String sourceId = strOrNull(body.get("sourceId"), "ext:" + database);
        if (database == null || database.isBlank()) {
            throw new BusinessException("database is required");
        }
        return BaseResponse.success(collector.collect(sourceId, host, port, database, username, password));
    }

    /** Real collection driven by a registered datasource (MYSQL). */
    @PostMapping("/collect-ds/{datasourceId}")
    public BaseResponse<Map<String, Object>> collectByDatasource(@PathVariable String datasourceId) {
        return BaseResponse.success(coordinator.collectDatasource(datasourceId));
    }

    /** Real collection for all registered MYSQL datasources. */
    @PostMapping("/collect-all")
    public BaseResponse<List<Map<String, Object>>> collectAll() {
        return BaseResponse.success(coordinator.collectAll());
    }

    /** Per-datasource overview (registered + collected counts + last sync). */
    @GetMapping("/overview")
    public BaseResponse<Map<String, Object>> overview() {
        return BaseResponse.success(coordinator.overview());
    }

    @GetMapping("/tables")
    public BaseResponse<List<MetaTableEntity>> tables(@RequestParam(required = false) String datasourceId) {
        return BaseResponse.success(datasourceId == null
                ? tableRepo.findAll()
                : tableRepo.findByDatasourceId(datasourceId));
    }

    @GetMapping("/columns")
    public BaseResponse<List<MetaColumnEntity>> columns(@RequestParam(required = false) String datasourceId) {
        return BaseResponse.success(datasourceId == null
                ? columnRepo.findAll()
                : columnRepo.findByDatasourceId(datasourceId));
    }

    @GetMapping("/changes")
    public BaseResponse<List<MetaChangeEntity>> changes(@RequestParam(required = false) String datasourceId) {
        if (datasourceId == null) {
            return BaseResponse.success(changeRepo.findAll());
        }
        return BaseResponse.success(changeRepo.findByDatasourceId(datasourceId));
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private String strOrNull(Object o, String def) {
        String v = str(o);
        return (v == null || v.isBlank()) ? def : v;
    }
}