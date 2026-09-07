package org.temporedata.modules.ops.alarm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * A triggered alarm record for a baseline + business date (P2-8).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "alarm_record")
public class AlarmRecordEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "baseline_id", nullable = false, length = 36)
    private String baselineId;

    @Column(name = "wf_instance_id", length = 36)
    private String wfInstanceId;

    @Column(name = "biz_date", length = 10)
    private String bizDate;

    @Column(name = "alarm_type", nullable = false, length = 16)
    private String alarmType; // MISS | DELAY | FAIL | RECOVERY

    @Column(name = "expect_finish_time", length = 32)
    private String expectFinishTime;

    @Column(name = "actual_finish_time", length = 32)
    private String actualFinishTime;

    @Column(length = 500)
    private String content;

    @Column(nullable = false, length = 16)
    private String status; // PENDING | SENT | ACK | CLOSED

    @Column(name = "create_time", length = 32)
    private String createTime;

    @Column(name = "ack_by", length = 32)
    private String ackBy;

    @Column(name = "ack_time", length = 32)
    private String ackTime;

    @Column(name = "close_time", length = 32)
    private String closeTime;
}