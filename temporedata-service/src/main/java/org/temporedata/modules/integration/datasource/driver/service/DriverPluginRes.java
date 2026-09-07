package org.temporedata.modules.integration.datasource.driver.service;

import lombok.Data;

/**
 * Datasource plugin view returned to the frontend.
 */
@Data
public class DriverPluginRes {

    private String id;
    private String dbType;
    private String name;
    private String version;
    private String driverPath;
    private String mvnGroup;
    private String mvnArtifact;
    private String mvnVersion;
    private String status;
    private Boolean builtin;
    private String createTime;
}