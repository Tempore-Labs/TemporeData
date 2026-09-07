package org.temporedata.modules.gov.meta.service;

import lombok.extern.slf4j.Slf4j;
import org.temporedata.modules.asset.mydata.entity.MydataEntity;
import org.temporedata.modules.asset.mydata.repository.MydataRepository;
import org.temporedata.modules.gov.catalog.entity.CatalogEntity;
import org.temporedata.modules.gov.catalog.repository.CatalogRepository;
import org.temporedata.modules.gov.meta.entity.MetaColumnEntity;
import org.temporedata.modules.gov.meta.entity.MetaTableEntity;
import org.temporedata.modules.gov.meta.repository.MetaColumnRepository;
import org.temporedata.modules.gov.meta.repository.MetaTableRepository;
import org.temporedata.modules.gov.meta.spi.MetaPostProcessor;
import org.temporedata.modules.integration.datasource.entity.DatasourceEntity;
import org.temporedata.modules.integration.datasource.repository.DatasourceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * CatalogAssetPostProcessor (design §6 catalog/asset automation): after a metadata
 * collection, reconciles the real tables/columns of a datasource into the asset catalog
 * ({@code zy_catalog}) as datasource -> schema -> table -> column nodes, tagging tables
 * and columns with their auto-derived data level. Manual governance overrides on the
 * catalog (comment / dataLevelId / dataCategoryId) are preserved because existing rows
 * are matched against stable metadata ids (with a name fallback) and updated in place.
 * <p>
 * Stale catalog rows whose metadata id is gone are removed. When
 * {@code temporedata.meta.catalog.mydata.owner} is configured, newly discovered tables are also
 * registered into the {@code zy_mydata} default authorization queue (disabled by default).
 */
@Slf4j
@Component
@Order(5)
public class CatalogAssetPostProcessor implements MetaPostProcessor {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MetaTableRepository tableRepo;
    private final MetaColumnRepository columnRepo;
    private final CatalogRepository catalogRepo;
    private final DatasourceRepository datasourceRepo;
    private final MydataRepository mydataRepo;

    @Value("${temporedata.meta.catalog.mydata.owner:}")
    private String mydataOwner;

    public CatalogAssetPostProcessor(MetaTableRepository tableRepo,
                                     MetaColumnRepository columnRepo,
                                     CatalogRepository catalogRepo,
                                     DatasourceRepository datasourceRepo,
                                     MydataRepository mydataRepo) {
        this.tableRepo = tableRepo;
        this.columnRepo = columnRepo;
        this.catalogRepo = catalogRepo;
        this.datasourceRepo = datasourceRepo;
        this.mydataRepo = mydataRepo;
    }

    @Override
    public void process(String datasourceId) {
        DatasourceEntity ds = datasourceRepo.findById(datasourceId).orElse(null);
        String dsName = ds != null && ds.getName() != null ? ds.getName() : datasourceId;

        List<MetaTableEntity> tables = tableRepo.findByDatasourceId(datasourceId);
        if (tables.isEmpty()) {
            return;
        }
        List<MetaColumnEntity> columns = columnRepo.findByDatasourceId(datasourceId);

        reconcileCatalog(datasourceId, dsName, tables, columns);
        autoRegisterMydata(datasourceId, dsName, tables);

        log.info("CatalogAsset: synced {} tables / {} columns into catalog for {}",
                tables.size(), columns.size(), datasourceId);
    }

    private void reconcileCatalog(String dsId, String dsName,
                                  List<MetaTableEntity> tables,
                                  List<MetaColumnEntity> columns) {
        String now = LocalDateTime.now().format(DTF);

        // existing catalog rows for this datasource, keyed for stable reuse
        Map<String, CatalogEntity> existing = index(dsId);
        Set<String> used = new HashSet<>();

        List<CatalogEntity> datasourceNodes = new ArrayList<>();
        Set<String> schemas = new HashSet<>();
        Map<String, List<MetaColumnEntity>> colsByTable = groupByTable(columns);

        for (MetaTableEntity t : tables) {
            String tKey = "T" + t.getId();
            used.add(tKey);
            CatalogEntity tn = existing.get(tKey);
            if (tn == null) {
                tn = existing.get("TN" + lower(t.getTableName()));
            }
            if (tn == null) {
                tn = new CatalogEntity();
            }
            if (tn.getType() == null) {
                tn.setType("table");
            }
            tn.setLabel(t.getTableName());
            tn.setDatasourceName(dsName);
            tn.setDatasourceId(dsId);
            tn.setSchemaName(t.getSchemaName());
            tn.setTableName(t.getTableName());
            tn.setTableId(t.getId());
            tn.setRowCount(t.getRowCount());
            tn.setComment(pick(t.getTableComment(), tn.getComment()));
            tn.setIsLeaf(false);
            tn.setSyncTime(now);
            if (tn.getDataLevelId() == null) {
                tn.setDataLevelId(t.getDataLevelCode());
            }
            datasourceNodes.add(tn);
            schemas.add(t.getSchemaName());

            List<MetaColumnEntity> tblCols = colsByTable.getOrDefault(t.getId(), new ArrayList<>());
            for (MetaColumnEntity c : tblCols) {
                String cKey = "C" + c.getId();
                used.add(cKey);
                CatalogEntity cn = existing.get(cKey);
                if (cn == null) {
                    cn = existing.get("CN" + c.getTableId() + ":" + lower(c.getColumnName()));
                }
                if (cn == null) {
                    cn = new CatalogEntity();
                }
                if (cn.getType() == null) {
                    cn.setType("column");
                }
                cn.setLabel(c.getColumnName());
                cn.setDatasourceName(dsName);
                cn.setDatasourceId(dsId);
                cn.setSchemaName(t.getSchemaName());
                cn.setTableName(t.getTableName());
                cn.setTableId(t.getId());
                cn.setColumnName(c.getColumnName());
                cn.setColumnId(c.getId());
                cn.setDataType(c.getColumnType());
                cn.setIsPrimaryKey(Boolean.TRUE.equals(c.getPrimaryKey()));
                cn.setIsNullable(Boolean.TRUE.equals(c.getNullable()));
                cn.setOrdinalPosition(c.getOrdinalPosition());
                cn.setComment(pick(c.getComment(), cn.getComment()));
                cn.setIsLeaf(true);
                cn.setSyncTime(now);
                if (cn.getDataLevelId() == null) {
                    cn.setDataLevelId(c.getDataLevelCode());
                }
                datasourceNodes.add(cn);
            }
        }

        // ensure datasource + schema nodes (created if missing)
        ensureDatasourceNode(existing, datasourceNodes, dsId, dsName, now);
        for (String schema : schemas) {
            if (schema == null) {
                continue;
            }
            CatalogEntity sn = existing.get("S" + dsId + ":" + lower(schema));
            if (sn == null) {
                sn = new CatalogEntity();
            }
            if (sn.getType() == null) {
                sn.setType("schema");
            }
            sn.setLabel(schema);
            sn.setDatasourceName(dsName);
            sn.setDatasourceId(dsId);
            sn.setSchemaName(schema);
            sn.setIsLeaf(false);
            sn.setSyncTime(now);
            datasourceNodes.add(sn);
        }

        catalogRepo.saveAll(datasourceNodes);

        // remove stale catalog rows (table/column metadata gone)
        List<CatalogEntity> stale = new ArrayList<>();
        for (CatalogEntity e : existing.values()) {
            if (e.getTableId() == null && e.getColumnId() == null) {
                continue; // datasource/schema nodes handled above by upsert
            }
            String key = e.getColumnId() != null ? "C" + e.getColumnId() : "T" + e.getTableId();
            if (!used.contains(key)) {
                stale.add(e);
            }
        }
        if (!stale.isEmpty()) {
            catalogRepo.deleteAll(stale);
        }
    }

    private Map<String, CatalogEntity> index(String dsId) {
        Map<String, CatalogEntity> m = new HashMap<>();
        for (CatalogEntity e : catalogRepo.findByDatasourceId(dsId)) {
            if (e.getTableId() != null) {
                m.put("T" + e.getTableId(), e);
                m.putIfAbsent("TN" + lower(e.getTableName()), e);
            }
            if (e.getColumnId() != null) {
                m.put("C" + e.getColumnId(), e);
                m.putIfAbsent("CN" + e.getTableId() + ":" + lower(e.getColumnName()), e);
            }
            if (e.getType() != null && e.getType().contains("atasource")) {
                m.put("DS" + dsId, e);
            }
            if (e.getType() != null && "schema".equals(e.getType()) && e.getSchemaName() != null) {
                m.put("S" + dsId + ":" + lower(e.getSchemaName()), e);
            }
        }
        return m;
    }

    private void ensureDatasourceNode(Map<String, CatalogEntity> existing, List<CatalogEntity> nodes,
                                      String dsId, String dsName, String now) {
        CatalogEntity node = existing.get("DS" + dsId);
        if (node == null) {
            node = new CatalogEntity();
        }
        node.setType("datasource");
        node.setLabel(dsName);
        node.setDatasourceName(dsName);
        node.setDatasourceId(dsId);
        node.setIsLeaf(false);
        node.setSyncTime(now);
        nodes.add(node);
    }

    private Map<String, List<MetaColumnEntity>> groupByTable(List<MetaColumnEntity> columns) {
        Map<String, List<MetaColumnEntity>> m = new HashMap<>();
        for (MetaColumnEntity c : columns) {
            if (c.getTableId() == null) {
                continue;
            }
            m.computeIfAbsent(c.getTableId(), k -> new ArrayList<>()).add(c);
        }
        return m;
    }

    private void autoRegisterMydata(String dsId, String dsName, List<MetaTableEntity> tables) {
        if (mydataOwner == null || mydataOwner.isBlank()) {
            return;
        }
        for (MetaTableEntity t : tables) {
            if (mydataRepo.existsByUserIdAndResourceId(mydataOwner, t.getId())) {
                continue;
            }
            mydataRepo.save(MydataEntity.builder()
                    .userId(mydataOwner)
                    .resourceType("table")
                    .resourceId(t.getId())
                    .resourceName(dsName + "." + t.getTableName())
                    .accessType("DEFAULT")
                    .createTime(LocalDateTime.now().format(DTF))
                    .build());
        }
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static String pick(String first, String fallback) {
        return first != null && !first.isBlank() ? first : fallback;
    }
}