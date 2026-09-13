package org.temporedata.modules.ops.engine.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Engine entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_engine")
public class EngineEntity {

    @Id @GeneratedValue(generator = "uuid2") @GenericGenerator(name = "uuid2", strategy = "uuid2") @Column(length = 36)
    private String id;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createDateTime;

    @Column(length = 32)
    private String createBy;

    @UpdateTimestamp
    private LocalDateTime updateDateTime;

    @Column(length = 32)
    private String updateBy;

    @Column(length = 32)
    private String tenantId;
    @Column
    private String name;
    @Column
    private String type;
    @Column
    private String jarPath;
    @Column
    private String mainClass;
    @Column
    private String sqlContent;
    @Column
    private String pythonFile;
    @Column
    private String flinkHome;
    @Column
    private String parallelism;
    @Column
    private String jobManagerMemory;
    @Column
    private String taskManagerMemory;
    @Column
    private Integer taskSlots;
    @Column
    private String savepointPath;
    @Column
    private String args;
    @Column
    private String conf;
    @Column
    private String status;
    @Column
    private String jobId;
    @Column
    private String lastRunTime;
    @Column
    private String duration;
    @Column
    private String errorMsg;
    @Column
    private String createTime;
    @Column
    private String master;
    @Column
    private String deployMode;
    @Column
    private String driverMemory;
    @Column
    private String executorMemory;
    @Column
    private Integer executorCores;
    @Column
    private Integer numExecutors;
    @Column
    private String appId;
}
