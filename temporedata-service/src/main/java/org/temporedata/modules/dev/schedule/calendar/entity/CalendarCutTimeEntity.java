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
 * Day-cut time of a business calendar (P1-4).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cal_cut_time")
public class CalendarCutTimeEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "calendar_id", nullable = false, length = 36)
    private String calendarId;

    @Column(name = "cut_hour", nullable = false)
    private Integer cutHour;

    @Column(name = "cut_minute", nullable = false)
    private Integer cutMinute;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}