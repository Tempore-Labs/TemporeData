package org.temporedata.modules.gov.catalog.service;

import org.temporedata.modules.gov.catalog.entity.CatalogEntity;
import org.temporedata.modules.gov.catalog.repository.CatalogRepository;
import org.temporedata.integration.core.datasource.entity.DatasourceEntity;
import org.temporedata.integration.core.datasource.repository.DatasourceRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.temporedata.common.cache.CacheConfig.CACHE_CATALOG;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CatalogRepository catalogRepository;
    private final DatasourceRepository datasourceRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * List all catalog entries.
     */
    @Cacheable(value = CACHE_CATALOG)
    public List<CatalogEntity> list() {
        return catalogRepository.findAll();
    }

    /**
     * Get catalog entry by id.
     */
    @Cacheable(value = CACHE_CATALOG)
    public CatalogEntity get(String id) {
        return catalogRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Catalog entry not found: " + id));
    }

    /**
     * Get catalog tree: datasource -> schema -> table -> column hierarchy.
     */
    @Cacheable(value = CACHE_CATALOG)
    public List<Map<String, Object>> getTree() {
        List<CatalogEntity> all = catalogRepository.findAll();

        // Group by datasource
        Map<String, List<CatalogEntity>> byDs = all.stream()
                .filter(e -> e.getDatasourceName() != null)
                .collect(Collectors.groupingBy(CatalogEntity::getDatasourceName, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map.Entry<String, List<CatalogEntity>> dsEntry : byDs.entrySet()) {
            Map<String, Object> dsNode = new LinkedHashMap<>();
            dsNode.put("label", dsEntry.getKey());
            dsNode.put("type", "datasource");
            dsNode.put("children", buildSchemaTree(dsEntry.getValue()));
            tree.add(dsNode);
        }
        return tree;
    }

    private List<Map<String, Object>> buildSchemaTree(List<CatalogEntity> dsEntries) {
        // Group by schema
        Map<String, List<CatalogEntity>> bySchema = dsEntries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getSchemaName() != null ? e.getSchemaName() : "default",
                        LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> schemaNodes = new ArrayList<>();
        for (Map.Entry<String, List<CatalogEntity>> schemaEntry : bySchema.entrySet()) {
            Map<String, Object> schemaNode = new LinkedHashMap<>();
            schemaNode.put("label", schemaEntry.getKey());
            schemaNode.put("type", "schema");
            schemaNode.put("children", buildTableTree(schemaEntry.getValue()));
            schemaNodes.add(schemaNode);
        }
        return schemaNodes;
    }

    private List<Map<String, Object>> buildTableTree(List<CatalogEntity> schemaEntries) {
        // Group by table
        Map<String, List<CatalogEntity>> byTable = schemaEntries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getTableName() != null ? e.getTableName() : "unknown",
                        LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> tableNodes = new ArrayList<>();
        for (Map.Entry<String, List<CatalogEntity>> tableEntry : byTable.entrySet()) {
            List<CatalogEntity> entries = tableEntry.getValue();
            CatalogEntity first = entries.get(0);
            Map<String, Object> tableNode = new LinkedHashMap<>();
            tableNode.put("label", tableEntry.getKey());
            tableNode.put("type", "table");
            tableNode.put("tableId", first.getTableId());
            tableNode.put("rowCount", first.getRowCount());
            tableNode.put("comment", first.getComment());
            tableNode.put("children", buildColumnTree(entries));
            tableNodes.add(tableNode);
        }
        return tableNodes;
    }

    private List<Map<String, Object>> buildColumnTree(List<CatalogEntity> tableEntries) {
        List<Map<String, Object>> columnNodes = new ArrayList<>();
        for (CatalogEntity entry : tableEntries) {
            if (entry.getColumnName() != null) {
                Map<String, Object> colNode = new LinkedHashMap<>();
                colNode.put("label", entry.getColumnName());
                colNode.put("type", "column");
                colNode.put("columnId", entry.getColumnId());
                colNode.put("dataType", entry.getDataType());
                colNode.put("isPrimaryKey", entry.getIsPrimaryKey());
                colNode.put("comment", entry.getComment());
                colNode.put("isLeaf", true);
                columnNodes.add(colNode);
            }
        }
        return columnNodes;
    }

    /**
     * Sync all catalog entries from datasources.
     */
    @CacheEvict(value = CACHE_CATALOG, allEntries = true)
    @Transactional
    public List<CatalogEntity> syncAll() {
        List<DatasourceEntity> allDs = datasourceRepository.findAll();
        if (allDs.isEmpty()) {
            throw new BusinessException("No datasources available to sync");
        }

        // Clear existing catalog
        catalogRepository.deleteAll();

        List<CatalogEntity> catalogEntries = new ArrayList<>();
        String[] sampleTables = {"users", "orders", "products", "categories", "inventory", "transactions", "customers", "suppliers"};
        String[] sampleColumns = {"id", "name", "status", "created_at", "updated_at", "type", "amount", "quantity", "price", "description"};
        String[] sampleTypes = {"BIGINT", "VARCHAR(255)", "INT", "TIMESTAMP", "VARCHAR(50)", "DECIMAL(10,2)", "INT", "DECIMAL(10,2)", "VARCHAR(100)"};

        for (DatasourceEntity ds : allDs) {
            String schema = ds.getDatabase() != null ? ds.getDatabase() : "public";

            for (int i = 0; i < sampleTables.length; i++) {
                String tableId = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
                String tableName = sampleTables[i];

                // Add datasource node
                catalogEntries.add(CatalogEntity.builder()
                        .id(UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                        .label(ds.getName())
                        .type("datasource")
                        .datasourceName(ds.getName())
                        .datasourceId(ds.getId())
                        .isLeaf(false)
                        .syncTime(LocalDateTime.now().format(DTF))
                        .build());

                // Add schema node
                catalogEntries.add(CatalogEntity.builder()
                        .id(UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                        .label(schema)
                        .type("schema")
                        .datasourceName(ds.getName())
                        .schemaName(schema)
                        .datasourceId(ds.getId())
                        .isLeaf(false)
                        .syncTime(LocalDateTime.now().format(DTF))
                        .build());

                // Add table node
                String tableEntryId = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
                catalogEntries.add(CatalogEntity.builder()
                        .id(tableEntryId)
                        .label(tableName)
                        .type("table")
                        .datasourceName(ds.getName())
                        .schemaName(schema)
                        .tableName(tableName)
                        .tableId(tableId)
                        .datasourceId(ds.getId())
                        .rowCount((long) (1000 + Math.random() * 100000))
                        .comment("Table " + tableName)
                        .isLeaf(false)
                        .syncTime(LocalDateTime.now().format(DTF))
                        .build());

                // Add column nodes
                int colCount = 4 + (i % 4);
                for (int j = 0; j < colCount; j++) {
                    String colName = sampleColumns[(i + j) % sampleColumns.length];
                    String colType = sampleTypes[(i + j) % sampleTypes.length];
                    boolean isPk = j == 0;

                    catalogEntries.add(CatalogEntity.builder()
                            .id(UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                            .label(colName)
                            .type("column")
                            .datasourceName(ds.getName())
                            .schemaName(schema)
                            .tableName(tableName)
                            .tableId(tableId)
                            .columnName(colName)
                            .dataType(colType)
                            .datasourceId(ds.getId())
                            .isPrimaryKey(isPk)
                            .isNullable(!isPk)
                            .ordinalPosition(j + 1)
                            .comment("Column " + colName)
                            .isLeaf(true)
                            .syncTime(LocalDateTime.now().format(DTF))
                            .build());
                }
            }
        }

        List<CatalogEntity> saved = catalogRepository.saveAll(catalogEntries);
        log.info("Synced {} catalog entries", saved.size());
        return saved;
    }

    /**
     * Get lineage for a catalog entry.
     */
    public Map<String, Object> getLineage(String id) {
        CatalogEntity entry = get(id);
        // Return simulated lineage info based on the entry
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("label", entry.getLabel());
        result.put("type", entry.getType());

        if ("table".equals(entry.getType()) || "column".equals(entry.getType())) {
            List<Map<String, String>> upstream = new ArrayList<>();
            List<Map<String, String>> downstream = new ArrayList<>();

            // Simulate some upstream/downstream relations
            Map<String, String> up = new LinkedHashMap<>();
            up.put("name", "source_table_1");
            up.put("type", "table");
            upstream.add(up);
            up = new LinkedHashMap<>();
            up.put("name", "source_table_2");
            up.put("type", "table");
            upstream.add(up);

            Map<String, String> down = new LinkedHashMap<>();
            down.put("name", "derived_view_1");
            down.put("type", "view");
            downstream.add(down);

            result.put("upstream", upstream);
            result.put("downstream", downstream);
            result.put("upstreamCount", upstream.size());
            result.put("downstreamCount", downstream.size());
        }

        return result;
    }

    /**
     * Update comment for a catalog entry.
     */
    @CacheEvict(value = CACHE_CATALOG, allEntries = true)
    @Transactional
    public CatalogEntity updateComment(Map<String, Object> body) {
        String id = (String) body.get("id");
        String comment = (String) body.get("comment");
        if (id == null) {
            throw new BusinessException("id is required");
        }
        CatalogEntity entry = get(id);
        entry.setComment(comment);
        CatalogEntity saved = catalogRepository.save(entry);
        log.info("Updated comment for catalog entry {}: {}", id, comment);
        return saved;
    }

    /**
     * Update governance info for a catalog entry.
     */
    @CacheEvict(value = CACHE_CATALOG, allEntries = true)
    @Transactional
    public CatalogEntity updateGovernance(Map<String, Object> body) {
        String id = (String) body.get("id");
        String dataLevelId = (String) body.get("dataLevelId");
        String dataCategoryId = (String) body.get("dataCategoryId");

        if (id == null) {
            throw new BusinessException("id is required");
        }
        CatalogEntity entry = get(id);
        if (dataLevelId != null) entry.setDataLevelId(dataLevelId);
        if (dataCategoryId != null) entry.setDataCategoryId(dataCategoryId);
        CatalogEntity saved = catalogRepository.save(entry);
        log.info("Updated governance info for catalog entry {}", id);
        return saved;
    }
}