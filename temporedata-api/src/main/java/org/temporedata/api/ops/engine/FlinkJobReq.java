package org.temporedata.api.ops.engine;

import lombok.Data;

/**
 * Flink job create / update request.
 */
@Data
public class FlinkJobReq {

    private String name;

    private String type; // JAR, SQL, PYTHON

    private String jarPath; // for JAR type

    private String mainClass; // for JAR type

    private String sqlContent; // for SQL type

    private String pythonFile; // for PYTHON type

    private String flinkHome; // FLINK_HOME path

    private String parallelism; // e.g. 2

    private String jobManagerMemory; // e.g. 1024m

    private String taskManagerMemory; // e.g. 1024m

    private Integer taskSlots; // e.g. 2

    private String savepointPath; // savepoint restore path

    private String args; // program arguments

    private String conf; // extra flink conf as JSON
}