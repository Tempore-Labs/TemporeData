package org.temporedata.modules.integration.datasource.driver.service;

import lombok.Data;

/**
 * Request to register an uploaded datasource plugin jar.
 */
@Data
public class DriverPluginReq {

    private String name;

    /** Optional; when empty, resolved from the loaded plugin. */
    private String dbType;

    private String version;

    /** Local filesystem path of an uploaded driver jar (fallback). */
    private String driverPath;

    /** Maven GAV coordinates; when set, dependencies are resolved via maven-resolver. */
    private String mvnGroup;
    private String mvnArtifact;
    private String mvnVersion;
}