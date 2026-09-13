package org.temporedata.metadata.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.metadata.drift.ColumnSig;
import org.temporedata.metadata.service.CrawlResult;
import org.temporedata.metadata.service.SchemaCrawlerService;

import java.util.ArrayList;
import java.util.List;

/**
 * [ENT-P0] Schema crawl &amp; drift REST endpoint.
 */
@RestController
@RequestMapping("/api/metadata")
public class MetadataCrawlController {

    private final SchemaCrawlerService schemaCrawlerService;

    public MetadataCrawlController(SchemaCrawlerService schemaCrawlerService) {
        this.schemaCrawlerService = schemaCrawlerService;
    }

    /** Submit an observed schema; snapshots it and reports the drift vs the previous version. */
    @PostMapping("/crawl")
    public BaseResponse<CrawlResult> crawl(@RequestBody CrawlRequest req) {
        List<ColumnSig> observed = new ArrayList<>();
        if (req.getColumns() != null) {
            for (Column reqCol : req.getColumns()) {
                observed.add(new ColumnSig(reqCol.getName(), reqCol.getType()));
            }
        }
        return BaseResponse.success(schemaCrawlerService.crawl(req.getDatasetId(), observed));
    }

    public static class CrawlRequest {
        private Long datasetId;
        private List<Column> columns;

        public Long getDatasetId() { return datasetId; }
        public void setDatasetId(Long datasetId) { this.datasetId = datasetId; }
        public List<Column> getColumns() { return columns; }
        public void setColumns(List<Column> columns) { this.columns = columns; }
    }

    public static class Column {
        private String name;
        private String type;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}