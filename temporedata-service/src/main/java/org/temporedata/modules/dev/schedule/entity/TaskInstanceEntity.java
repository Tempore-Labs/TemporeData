package org.temporedata.modules.dev.schedule.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Task instance entity - one row per trigger/fire (P0-1 scheduler engine).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zy_task_instance")
public class TaskInstanceEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "instance_no", nullable = false)
    private Long instanceNo;

    @Column(name = "trigger_time")
    private LocalDateTime triggerTime;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "finish_time")
    private LocalDateTime finishTime;

    @Column(name = "biz_date", length = 10)
    private String bizDate;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "trigger_node", length = 32)
    private String triggerNode;

    @Column(name = "result_msg", columnDefinition = "TEXT")
    private String resultMsg;

    @Column(nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}