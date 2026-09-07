package org.temporedata.modules.dev.schedule.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Task definition entity - a schedulable unit (P0-1 scheduler engine).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zy_task_define")
public class TaskDefineEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "task_type", nullable = false, length = 32)
    private String taskType;

    @Column(name = "target_ref", length = 64)
    private String targetRef;

    @Column(name = "cron_expression", nullable = false, length = 64)
    private String cronExpression;

    @Column(name = "biz_date_mode", nullable = false, length = 16)
    private String bizDateMode;

    @Column(name = "calendar_id", length = 36)
    private String calendarId;

    @Column(length = 64)
    private String timezone;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "params_json", columnDefinition = "TEXT")
    private String paramsJson;

    @Column(length = 64)
    private String owner;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "create_by", length = 32)
    private String createBy;

    @Column(name = "update_by", length = 32)
    private String updateBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}