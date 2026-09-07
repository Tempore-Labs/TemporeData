package org.temporedata.modules.dev.query.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Query entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_query")
public class QueryEntity {

    @Id @GeneratedValue(generator = "uuid2") @GenericGenerator(name = "uuid2", strategy = "uuid2") @Column(length = 32)
    private String id;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createDateTime;

    @Column(length = 32)
    private String createBy;

    @UpdateTimestamp
    private LocalDateTime updateDateTime;

    @Column(length = 32)
    private String updateBy;

    @Column(length = 32)
    private String tenantId;
    @Column
    private String datasourceName;
    @Column(name = "sql_text", columnDefinition = "TEXT")
    private String sql;
    @Column
    private String status;
    @Column
    private long durationMs;
    @Column
    private String createTime;
    @Column(columnDefinition = "TEXT")
    private String errorMsg;
    @Column
    private String datasourceId;
    @Column
    private int rowCount;

    /** Real, governed result set, persisted as JSON for history/storage. */
    @Column(name = "columns_json", columnDefinition = "TEXT")
    private String columnsJson;

    @Column(name = "rows_json", columnDefinition = "TEXT")
    private String rowsJson;

    /** Transient response projection (populated on execute; not persisted). */
    @Transient
    private List<String> columns;

    @Transient
    private List<Map<String, Object>> rows;
}
