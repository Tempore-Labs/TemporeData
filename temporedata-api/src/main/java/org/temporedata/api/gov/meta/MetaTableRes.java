package org.temporedata.api.gov.meta;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Table metadata response with column details.
 */
@Data
public class MetaTableRes {

    private String id;
    private String datasourceId;
    private String datasourceName;
    private String schemaName;
    private String tableName;
    private String comment;
    private Long rowCount;
    private Long dataSize;
    private List<MetaColumnRes> columns;
    private Boolean hasLineage;
    private Integer upstreamCount;
    private Integer downstreamCount;
    private LocalDateTime syncTime;
    private LocalDateTime createTime;
}