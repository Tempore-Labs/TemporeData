package org.temporedata.modules.dev.schedule.calendar.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Single day override of a business calendar (P1-4).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cal_day_meta")
public class CalendarDayEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "calendar_id", nullable = false, length = 36)
    private String calendarId;

    @Column(name = "biz_date", nullable = false, length = 10)
    private String bizDate;

    @Column(name = "is_workday", nullable = false)
    private Boolean workday;

    @Column(name = "day_type", nullable = false, length = 16)
    private String dayType;

    @Column(length = 128)
    private String remark;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}