package org.temporedata.api.gov.meta;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SqlLineageRes {

    private String sqlType; // INSERT_SELECT, CTAS, SELECT
    private String outputTable;
    private String outputAlias;
    private List<String> inputTables = new ArrayList<>();
    private List<FieldMapping> fieldMappings = new ArrayList<>();
    private List<TableNode> nodes = new ArrayList<>();
    private List<TableEdge> edges = new ArrayList<>();

    @Data
    public static class FieldMapping {
        private String sourceTable;
        private String sourceColumn;
        private String targetColumn;
        private String transformation;
    }

    @Data
    public static class TableNode {
        private String id;
        private String name;
        private String category; // "output", "input"
        private boolean registered; // exists in meta tables
    }

    @Data
    public static class TableEdge {
        private String source;
        private String target;
        private String relationType;
        private String relationName;
        private List<FieldMapping> fieldMappings;
    }
}