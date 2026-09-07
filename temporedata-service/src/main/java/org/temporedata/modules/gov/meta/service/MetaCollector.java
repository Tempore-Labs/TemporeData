package org.temporedata.modules.gov.meta.service;

import org.temporedata.modules.gov.meta.entity.MetaChangeEntity;
import org.temporedata.modules.gov.meta.entity.MetaColumnEntity;
import org.temporedata.modules.gov.meta.entity.MetaSyncLogEntity;
import org.temporedata.modules.gov.meta.entity.MetaTableEntity;
import org.temporedata.modules.gov.meta.repository.MetaChangeRepository;
import org.temporedata.modules.gov.meta.repository.MetaColumnRepository;
import org.temporedata.modules.gov.meta.repository.MetaSyncLogRepository;
import org.temporedata.modules.gov.meta.repository.MetaTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * MetaCollector (P1, design §4/§5): reads REAL schema via {@code DatabaseMetaData},
 * persists into the layered tables, computes a per-table structure {@code checksum}
 * for increment sync (unchanged tables are skipped), and records a sync session log.
 * Replaces the old sample-generation sync.
 */
@Service
public class MetaCollector {

    private final MetaTableRepository tableRepo;
    private final MetaColumnRepository columnRepo;
    private final MetaSyncLogRepository syncLogRepo;
    private final MetaChangeRepository changeRepo;
    private final MetaPipeline pipeline;

    private static final String MYSQL_URL =
            "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MetaCollector(MetaTableRepository tableRepo,
                         MetaColumnRepository columnRepo,
                         MetaSyncLogRepository syncLogRepo,
                         MetaChangeRepository changeRepo,
                         MetaPipeline pipeline) {
        this.tableRepo = tableRepo;
        this.columnRepo = columnRepo;
        this.syncLogRepo = syncLogRepo;
        this.changeRepo = changeRepo;
        this.pipeline = pipeline;
    }

    /** Collect real MySQL metadata with incremental (checksum) sync. Return a summary map. */
    @Transactional
    public Map<String, Object> collect(String sourceId, String host, int port,
                                       String database, String username, String password) {
        String started = LocalDateTime.now().format(DTF);
        String syncLogId = null;
        List<String> added = new ArrayList<>();
        List<String> structChanged = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        int tableCount = 0;
        int columnCount = 0;
        int unchanged = 0;
        Set<String> tableNames = new LinkedHashSet<>();
        List<String> errors = new ArrayList<>();

        Map<String, List<MetaColumnEntity>> newColumns = new LinkedHashMap<>();

        java.sql.Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = java.sql.DriverManager.getConnection(String.format(MYSQL_URL, host, port, database),
                    username, password);
            java.sql.DatabaseMetaData md = conn.getMetaData();
            String now = LocalDateTime.now().format(DTF);

            Map<String, MetaTableEntity> existing = existingTables(sourceId);
            Set<String> seenTables = new LinkedHashSet<>();
            Map<String, MetaTableEntity> tablesToSave = new LinkedHashMap<>();
            List<MetaTableEntity> tablesToDelete = new ArrayList<>();
            List<MetaColumnEntity> orphanColumns = new ArrayList<>();

            try (java.sql.ResultSet rsT = md.getTables(database, null, "%", new String[]{"TABLE"})) {
                while (rsT.next()) {
                    String tableName = rsT.getString("TABLE_NAME");
                    if (tableName == null) {
                        continue;
                    }
                    seenTables.add(tableName);
                    tableNames.add(tableName);

                    Set<String> pks = primaryKeys(md, database, tableName);
                    List<MetaColumnEntity> cols = new ArrayList<>();
                    try (java.sql.ResultSet rsC = md.getColumns(database, null, tableName, "%")) {
                        while (rsC.next()) {
                            String col = rsC.getString("COLUMN_NAME");
                            if (col == null) {
                                continue;
                            }
                            boolean pk = pks.contains(col);
                            cols.add(MetaColumnEntity.builder()
                                    .datasourceId(sourceId)
                                    .tableName(tableName)
                                    .columnName(col)
                                    .columnType(rsC.getString("TYPE_NAME"))
                                    .columnSize(toInt(rsC.getObject("COLUMN_SIZE")))
                                    .nullable("YES".equalsIgnoreCase(rsC.getString("IS_NULLABLE")))
                                    .defaultValue(rsC.getString("COLUMN_DEF"))
                                    .comment(rsC.getString("REMARKS"))
                                    .primaryKey(pk)
                                    .ordinalPosition(toInt(rsC.getObject("ORDINAL_POSITION")))
                                    .sensitiveFlag(autoSensitive(col, rsC.getString("TYPE_NAME")))
                                    .status("ACTIVE")
                                    .build());
                        }
                    }

                    String checksum = structureChecksum(tableName, cols);
                    String comment = rsT.getString("REMARKS");
                    MetaTableEntity existingRow = existing.get(tableName);

                    if (existingRow != null && checksum.equals(existingRow.getChecksum())) {
                        // unchanged -> keep the stored table + its columns untouched (incremental)
                        unchanged++;
                        continue;
                    }

                    // changed (reuse id) or newly added -> reconcile persist
                    MetaTableEntity t;
                    if (existingRow != null) {
                        t = existingRow;
                        t.setSchemaName(database);
                        t.setTableComment(comment);
                        t.setStatus("ACTIVE");
                        t.setLastSyncTime(now);
                        t.setChecksum(checksum);
                        structChanged.add(tableName);
                        // purge stale columns of this table before re-inserting fresh ones
                        orphanColumns.addAll(columnRepo.findByTableId(t.getId()));
                    } else {
                        t = MetaTableEntity.builder()
                                .datasourceId(sourceId)
                                .schemaName(database)
                                .tableName(tableName)
                                .tableComment(comment)
                                .status("ACTIVE")
                                .lastSyncTime(now)
                                .checksum(checksum)
                                .build();
                        added.add(tableName);
                    }
                    tablesToSave.put(tableName, t);
                    newColumns.put(tableName, cols);
                    tableCount++;
                    columnCount += cols.size();
                }
            }

            // P3: REMOVE tables present in store but absent from the live schema
            for (String oldTable : existing.keySet()) {
                if (!seenTables.contains(oldTable)) {
                    MetaTableEntity t = existing.get(oldTable);
                    removed.add(oldTable);
                    tablesToDelete.add(t);
                    orphanColumns.addAll(columnRepo.findByTableId(t.getId()));
                }
            }

            // Reconcile persist: drop stale/removed rows first, save tables (assign ids), then columns
            if (!orphanColumns.isEmpty()) {
                columnRepo.deleteAll(orphanColumns);
            }
            if (!tablesToDelete.isEmpty()) {
                tableRepo.deleteAll(tablesToDelete);
            }
            if (!tablesToSave.isEmpty()) {
                Map<String, MetaTableEntity> saved = new LinkedHashMap<>();
                for (MetaTableEntity t : tableRepo.saveAll(tablesToSave.values())) {
                    saved.put(t.getTableName(), t);
                }
                List<MetaColumnEntity> colsToSave = new ArrayList<>();
                for (Map.Entry<String, List<MetaColumnEntity>> e : newColumns.entrySet()) {
                    MetaTableEntity t = saved.get(e.getKey());
                    if (t == null) {
                        continue;
                    }
                    String tid = t.getId();
                    for (MetaColumnEntity c : e.getValue()) {
                        c.setTableId(tid);
                        colsToSave.add(c);
                    }
                }
                columnRepo.saveAll(colsToSave);
            }

            persistChanges(sourceId, added, structChanged, removed);

            // P2: post-processing pipeline (sensitivity/level classification + default masking rules)
            pipeline.process(sourceId);

            syncLogId = record(sourceId, "FULL", "SUCCESS", started,
                    tableCount, columnCount, unchanged, null);
        } catch (Exception e) {
            errors.add(e.getMessage());
            record(sourceId, "FULL", "FAILED", started, tableCount, columnCount, unchanged,
                    String.join(" | ", errors));
            throw new RuntimeException("Meta collect failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) { }
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("syncLogId", syncLogId);
        out.put("tables", tableCount);
        out.put("columns", columnCount);
        out.put("unchangedTables", unchanged);
        out.put("totalTables", tableNames.size());
        out.put("tableNames", new ArrayList<>(tableNames));
        out.put("added", added == null ? new ArrayList<>() : new ArrayList<>(added));
        out.put("structChanged", structChanged == null ? new ArrayList<>() : new ArrayList<>(structChanged));
        out.put("removed", removed == null ? new ArrayList<>() : new ArrayList<>(removed));
        return out;
    }

    private void persistChanges(String sourceId, List<String> added,
                                List<String> structChanged, List<String> removed) {
        String now = LocalDateTime.now().format(DTF);
        List<MetaChangeEntity> changes = new ArrayList<>();
        for (String t : added) {
            changes.add(change(sourceId, t, "ADD", now));
        }
        for (String t : structChanged) {
            changes.add(change(sourceId, t, "CHANGE", now));
        }
        for (String t : removed) {
            changes.add(change(sourceId, t, "REMOVE", now));
        }
        if (!changes.isEmpty()) {
            changeRepo.saveAll(changes);
        }
    }

    private MetaChangeEntity change(String sourceId, String tableName, String type, String now) {
        return MetaChangeEntity.builder()
                .datasourceId(sourceId)
                .tableName(tableName)
                .changeType(type)
                .syncedAt(now)
                .build();
    }

    private Map<String, MetaTableEntity> existingTables(String sourceId) {
        Map<String, MetaTableEntity> m = new LinkedHashMap<>();
        for (MetaTableEntity t : tableRepo.findByDatasourceId(sourceId)) {
            m.put(t.getTableName(), t);
        }
        return m;
    }

    private String record(String sourceId, String type, String status, String started,
                          int tables, int cols, int unchanged, String errors) {
        MetaSyncLogEntity log = MetaSyncLogEntity.builder()
                .datasourceId(sourceId)
                .syncType(type)
                .status(status)
                .startedAt(started)
                .finishedAt(LocalDateTime.now().format(DTF))
                .tablesDiscovered(tables)
                .columnsDiscovered(cols)
                .unchangedTables(unchanged)
                .errors(errors)
                .build();
        return syncLogRepo.save(log).getId();
    }

    /** Deterministic structural hash: sorted "col:type:nullable" lines + table name. */
    private String structureChecksum(String tableName, List<MetaColumnEntity> cols) {
        TreeSet<String> lines = new TreeSet<>();
        for (MetaColumnEntity c : cols) {
            lines.add(c.getColumnName() + ":" + c.getColumnType() + ":" + (c.getNullable() != null && c.getNullable()));
        }
        return md5(tableName + lines);
    }

    private Set<String> primaryKeys(java.sql.DatabaseMetaData md, String db, String table) {
        Set<String> out = new LinkedHashSet<>();
        try (java.sql.ResultSet rs = md.getPrimaryKeys(db, null, table)) {
            while (rs.next()) {
                out.add(rs.getString("COLUMN_NAME"));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private Integer toInt(Object o) {
        return o == null ? null : ((Number) o).intValue();
    }

    private boolean autoSensitive(String columnName, String type) {
        if (columnName == null) {
            return false;
        }
        String c = columnName.toLowerCase();
        return c.contains("password") || c.contains("phone") || c.contains("mobile")
                || c.contains("id_card") || c.contains("email") || c.contains("bank_card")
                || c.contains("salary") || (type != null && type.toLowerCase().contains("encrypted"));
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : d) {
                sb.append(String.format("%02x", x));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}