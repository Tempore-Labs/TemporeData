package org.temporedata.modules.ops.alarm.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator; import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*; import java.time.LocalDateTime;

/** Alarm entity. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_alarm")
public class AlarmEntity {

    @Id @GeneratedValue(generator = "uuid2") @GenericGenerator(name = "uuid2", strategy = "uuid2") @Column(length = 32)
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
    private String eventType;
    @Column
    private String channels;
    @Column
    private String email;
    @Column
    private String webhookUrl;
    @Column
    private Boolean enabled;
    @Column
    private String createTime;
    @Column
    private String message;
    @Column
    private String status;
    @Column
    private String sendTime;
}
