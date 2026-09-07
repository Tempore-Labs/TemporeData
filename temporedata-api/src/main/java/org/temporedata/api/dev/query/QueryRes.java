package org.temporedata.api.dev.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SQL query result.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryRes {

    private List<String> columns;

    private List<List<Object>> rows;

    private int rowCount;

    private long durationMs;
}