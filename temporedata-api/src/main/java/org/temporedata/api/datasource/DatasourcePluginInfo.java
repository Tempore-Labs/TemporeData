package org.temporedata.api.datasource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Read-only descriptor of a supported datasource plugin, exposed to the
 * frontend plugin list. Derivable entirely from a {@link DatasourceProcessor}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasourcePluginInfo {

    /** Upper-case type key, e.g. MYSQL. */
    private String type;

    /** Display name, e.g. MySQL. */
    private String name;

    /** Default port. */
    private int defaultPort;

    /** Whether this type ships with the platform (true) or is an uploaded plugin. */
    private boolean builtin;
}