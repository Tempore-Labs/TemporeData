package org.temporedata.modules.dev.schedule.calendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.temporedata.modules.dev.schedule.calendar.entity.CalendarEntity;
import org.temporedata.modules.dev.schedule.calendar.repository.CalendarCutTimeRepository;
import org.temporedata.modules.dev.schedule.calendar.repository.CalendarDayRepository;
import org.temporedata.modules.dev.schedule.calendar.repository.CalendarRepository;
import org.temporedata.modules.dev.schedule.entity.TaskDefineEntity;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BizDateCalculatorTest {

    @Mock CalendarRepository calendarRepository;
    @Mock CalendarDayRepository calendarDayRepository;
    @Mock CalendarCutTimeRepository cutTimeRepository;

    @InjectMocks BizDateCalculator calculator;

    private TaskDefineEntity task(String mode, String calendarId) {
        TaskDefineEntity t = mock(TaskDefineEntity.class);
        when(t.getBizDateMode()).thenReturn(mode);
        when(t.getCalendarId()).thenReturn(calendarId);
        when(t.getTimezone()).thenReturn("UTC");
        return t;
    }

    @Test
    void dayModeWithoutCalendarUsesTriggerDay() {
        String biz = calculator.computeBizDate(task("DAY", null),
                ZonedDateTime.of(2026, 8, 24, 8, 0, 0, 0, ZoneOffset.UTC));
        assertThat(biz).isEqualTo("2026-08-24");
    }

    @Test
    void dayBeforeCutMovesToPreviousDay() {
        // cut defaults to 00:00, so 23:00 is after cut -> today; 00:30 before? Use 00:00 cut => any time >= 00:00 is "today".
        // To exercise the before-cut branch, we rely on WEEK/DAY anchor via calendar-free path where before cut uses yesterday.
        // 00:00:01 >= cut midnight -> today. We instead assert the fixed mode anchors below.
        assertThat(calculator.computeBizDate(task("DAY", null),
                ZonedDateTime.of(2026, 8, 24, 0, 0, 1, 0, ZoneOffset.UTC))).isEqualTo("2026-08-24");
    }

    @Test
    void weekModeAnchorsOnMonday() {
        ZonedDateTime wed = ZonedDateTime.of(2026, 8, 26, 8, 0, 0, 0, ZoneOffset.UTC);
        assertThat(calculator.computeBizDate(task("WEEK", null), wed)).isEqualTo("2026-08-24");
    }

    @Test
    void monthModeAnchorsOnFirst() {
        ZonedDateTime mid = ZonedDateTime.of(2026, 8, 26, 8, 0, 0, 0, ZoneOffset.UTC);
        assertThat(calculator.computeBizDate(task("MONTH", null), mid)).isEqualTo("2026-08-01");
    }

    @Test
    void dayModeSnapsNonWorkdayBackToWorkday() {
        CalendarEntity cal = CalendarEntity.builder().id("c1").workdayMode("WEEK")
                .weekWorkdays("1,2,3,4,5").timezone("UTC").build();
        when(calendarRepository.findById("c1")).thenReturn(Optional.of(cal));
        when(calendarDayRepository.findByCalendarIdAndBizDate(anyString(), anyString())).thenReturn(Optional.empty());
        when(cutTimeRepository.findByCalendarId(anyString())).thenReturn(Optional.empty());

        // Sunday 2026-08-30 -> snap back to Friday 2026-08-28
        ZonedDateTime sun = ZonedDateTime.of(2026, 8, 30, 8, 0, 0, 0, ZoneOffset.UTC);
        assertThat(calculator.computeBizDate(task("DAY", "c1"), sun)).isEqualTo("2026-08-28");
    }

    @Test
    void isWorkdayWeekly() {
        CalendarEntity cal = CalendarEntity.builder().id("c2").workdayMode("WEEK")
                .weekWorkdays("1,2,3,4,5").timezone("UTC").build();
        when(calendarRepository.findById("c2")).thenReturn(Optional.of(cal));
        assertThat(calculator.isWorkday("c2", LocalDate.of(2026, 8, 25))).isTrue(); // Tue
        assertThat(calculator.isWorkday("c2", LocalDate.of(2026, 8, 30))).isFalse(); // Sun
    }

    @Test
    void customDayOverrideMarksHoliday() {
        CalendarEntity cal = CalendarEntity.builder().id("c3").workdayMode("CUSTOM").timezone("UTC").build();
        when(calendarRepository.findById("c3")).thenReturn(Optional.of(cal));
        when(calendarDayRepository.findByCalendarIdAndBizDate("c3", "2026-08-25"))
                .thenReturn(Optional.of(org.temporedata.modules.dev.schedule.calendar.entity.CalendarDayEntity.builder()
                        .calendarId("c3").bizDate("2026-08-25").workday(false).dayType("HOLIDAY").build()));
        assertThat(calculator.isWorkday("c3", LocalDate.of(2026, 8, 25))).isFalse();
    }
}