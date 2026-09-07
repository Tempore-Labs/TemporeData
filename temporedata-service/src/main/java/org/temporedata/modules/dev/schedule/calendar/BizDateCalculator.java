package org.temporedata.modules.dev.schedule.calendar;

import org.temporedata.modules.dev.schedule.calendar.entity.CalendarDayEntity;
import org.temporedata.modules.dev.schedule.calendar.entity.CalendarEntity;
import org.temporedata.modules.dev.schedule.calendar.repository.CalendarCutTimeRepository;
import org.temporedata.modules.dev.schedule.calendar.repository.CalendarDayRepository;
import org.temporedata.modules.dev.schedule.calendar.repository.CalendarRepository;
import org.temporedata.modules.dev.schedule.entity.TaskDefineEntity;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Business date calculator (P1-4).
 *
 * Semantics:
 * - DAY  : biz_date = the trigger day (or previous day when before the day-cut),
 *          snapped back to the nearest workday of the calendar.
 * - WEEK : biz_date = the ISO Monday of the period (fixed period anchor).
 * - MONTH: biz_date = the 1st of the month of the period.
 */
@Component
public class BizDateCalculator {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CalendarRepository calendarRepository;
    private final CalendarDayRepository calendarDayRepository;
    private final CalendarCutTimeRepository cutTimeRepository;

    public BizDateCalculator(CalendarRepository calendarRepository,
                             CalendarDayRepository calendarDayRepository,
                             CalendarCutTimeRepository cutTimeRepository) {
        this.calendarRepository = calendarRepository;
        this.calendarDayRepository = calendarDayRepository;
        this.cutTimeRepository = cutTimeRepository;
    }

    /**
     * Derive the business date for a task triggered at <code>now</code>.
     *
     * @return "yyyy-MM-dd" or null when task has no biz-date mode
     */
    public String computeBizDate(TaskDefineEntity task, ZonedDateTime now) {
        String mode = task.getBizDateMode();
        if (mode == null || "NONE".equalsIgnoreCase(mode)) {
            return null;
        }
        ZoneId zone = toZone(task.getTimezone());
        LocalTime cut = cutTime(task.getCalendarId());
        LocalDateTimeTrigger t = new LocalDateTimeTrigger(now, zone, cut);
        CalendarEntity cal = calendarById(task.getCalendarId());

        LocalDate candidate = t.periodStart(); // base (today) or yesterday if before cut
        switch (mode.toUpperCase()) {
            case "WEEK":
                candidate = candidate.with(DayOfWeek.MONDAY);
                break;
            case "MONTH":
                candidate = candidate.withDayOfMonth(1);
                break;
            case "DAY":
            default:
                candidate = snapToWorkday(cal, candidate);
                break;
        }
        return candidate.format(DTF);
    }

    public boolean isWorkday(String calendarId, LocalDate date) {
        CalendarEntity cal = calendarById(calendarId);
        return isWorkday(cal, date);
    }

    private boolean isWorkday(CalendarEntity cal, LocalDate date) {
        if (cal == null) return true;
        if ("WEEK".equalsIgnoreCase(cal.getWorkdayMode())) {
            // week_workdays: "1,2,3,4,5" where 1=Mon..7=Sun
            String list = cal.getWeekWorkdays() == null ? "1,2,3,4,5" : cal.getWeekWorkdays();
            int dow = date.getDayOfWeek().getValue();
            for (String part : list.split(",")) {
                if (Integer.toString(dow).equals(part.trim())) return true;
            }
            return false;
        }
        Optional<CalendarDayEntity> override = calendarDayRepository
                .findByCalendarIdAndBizDate(cal.getId(), date.toString());
        return override.map(CalendarDayEntity::getWorkday).orElse(true);
    }

    private LocalDate snapToWorkday(CalendarEntity cal, LocalDate d) {
        if (isWorkday(cal, d)) return d;
        for (int i = 1; i <= 31; i++) {
            LocalDate prev = d.minusDays(i);
            if (isWorkday(cal, prev)) return prev;
        }
        return d;
    }

    private LocalTime cutTime(String calendarId) {
        if (calendarId == null) return LocalTime.MIDNIGHT;
        return cutTimeRepository.findByCalendarId(calendarId)
                .map(c -> LocalTime.of(c.getCutHour(), c.getCutMinute()))
                .orElse(LocalTime.MIDNIGHT);
    }

    private CalendarEntity calendarById(String calendarId) {
        if (calendarId == null) return null;
        return calendarRepository.findById(calendarId).orElse(null);
    }

    private ZoneId toZone(String tz) {
        try {
            return tz == null || tz.isBlank() ? ZoneId.systemDefault() : ZoneId.of(tz);
        } catch (Exception e) {
            return ZoneId.systemDefault();
        }
    }

    /** Holds the zone-local trigger instant and cut, exposing the period start date. */
    private static final class LocalDateTimeTrigger {
        private final ZonedDateTime now;
        private final ZoneId zone;
        private final LocalTime cut;

        LocalDateTimeTrigger(ZonedDateTime now, ZoneId zone, LocalTime cut) {
            this.now = now;
            this.zone = zone;
            this.cut = cut;
        }

        LocalDate periodStart() {
            LocalDate base = now.withZoneSameInstant(zone).toLocalDate();
            boolean beforeCut = now.withZoneSameInstant(zone).toLocalTime().isBefore(cut);
            return beforeCut ? base.minusDays(1) : base;
        }
    }
}