package org.temporedata.modules.ops.alarm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * Baseline rule: expected absolute finish time for a workflow (P2-8).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "alarm_baseline")
public class AlarmBaselineEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "workflow_id", nullable = false, length = 36)
    private String workflowId;

    @Column(name = "calendar_id", length = 36)
    private String calendarId;

    @Column(name = "biz_date_mode", length = 16)
    private String bizDateMode;

    @Column(length = 64)
    private String timezone;

    @Column(name = "expect_time", nullable = false, length = 8)
    private String expectTime;

    @Column(name = "trigger_type", length = 16)
    private String triggerType;

    @Column(name = "grace_seconds")
    private Integer graceSeconds;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "notify_channels", length = 255)
    private String notifyChannels;

    @Column(length = 32)
    private String operator;

    @Column(length = 255)
    private String remark;

    @Column(name = "create_time", length = 32)
    private String createTime;
}