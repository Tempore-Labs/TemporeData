package org.temporedata.api.gov.security;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of a governed data access: pruned columns + desensitized rows
 * (or denied flag when access is not allowed).
 */
@Data
public class GovernedResult {

    /** Columns actually returned after pruning. */
    private List<String> columns = new ArrayList<>();

    /** Rows after column pruning and desensitization. */
    private List<Map<String, Object>> rows = new ArrayList<>();

    /** true when access was denied (fail-closed) -> empty result. */
    private boolean denied;
}