package org.temporedata.api.ops.engine;

import lombok.Data;

/**
 * Spark job create / update request.
 */
@Data
public class SparkJobReq {

    private String name;

    private String type; // SPARK_SQL, SPARK_JAR, PYSPARK

    private String mainClass; // for JAR type

    private String jarPath; // path to JAR file

    private String sqlContent; // for SPARK_SQL type

    private String pythonFile; // for PYSPARK type

    private String master; // local[*], yarn, spark://host:7077

    private String deployMode; // client, cluster

    private String driverMemory; // e.g. 1g

    private String executorMemory; // e.g. 1g

    private Integer executorCores; // e.g. 1

    private Integer numExecutors; // e.g. 2

    private String args; // program arguments

    private String conf; // extra spark conf as JSON
}