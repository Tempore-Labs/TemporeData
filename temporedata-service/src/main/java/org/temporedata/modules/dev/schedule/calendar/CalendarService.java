package org.temporedata.modules.dev.schedule.calendar;

import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.dev.schedule.CalendarDaySaveItem;
import org.temporedata.api.dev.schedule.CalendarDayView;
import org.temporedata.api.dev.schedule.CalendarReq;
import org.temporedata.api.dev.schedule.CalendarRes;
import org.temporedata.modules.dev.schedule.calendar.entity.CalendarCutTimeEntity;
import org.temporedata.modules.dev.schedule.calendar.entity.CalendarDayEntity;
import org.temporedata.modules.dev.schedule.calendar.entity.CalendarEntity;
import org.temporedata.modules.dev.schedule.calendar.repository.CalendarCutTimeRepository;
import org.temporedata.modules.dev.schedule.calendar.repository.CalendarDayRepository;
import org.temporedata.modules.dev.schedule.calendar.repository.CalendarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CalendarService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CalendarRepository calendarRepository;
    private final CalendarDayRepository calendarDayRepository;
    private final CalendarCutTimeRepository cutTimeRepository;
    private final BizDateCalculator bizDateCalculator;

    public CalendarService(CalendarRepository calendarRepository,
                           CalendarDayRepository calendarDayRepository,
                           CalendarCutTimeRepository cutTimeRepository,
                           BizDateCalculator bizDateCalculator) {
        this.calendarRepository = calendarRepository;
        this.calendarDayRepository = calendarDayRepository;
        this.cutTimeRepository = cutTimeRepository;
        this.bizDateCalculator = bizDateCalculator;
    }

    // ---- CRUD ----

    @Transactional(readOnly = true)
    public List<CalendarRes> list() {
        return calendarRepository.findAll().stream().map(this::toRes).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CalendarRes get(String id) {
        return toRes(find(id));
    }

    @Transactional
    public CalendarRes create(CalendarReq req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BusinessException("Calendar name is required");
        }
        CalendarEntity entity = CalendarEntity.builder()
                .name(req.getName())
                .description(req.getDescription())
                .workdayMode(req.getWorkdayMode() == null ? "CUSTOM" : req.getWorkdayMode())
                .timezone(req.getTimezone() == null || req.getTimezone().isBlank() ? "Asia/Shanghai" : req.getTimezone())
                .weekWorkdays(req.getWeekWorkdays() == null ? "1,2,3,4,5" : req.getWeekWorkdays())
                .build();
        CalendarEntity saved = calendarRepository.save(entity);
        saveCut(saved.getId(), req.getCutHour(), req.getCutMinute());
        return toRes(saved);
    }

    @Transactional
    public CalendarRes update(String id, CalendarReq req) {
        CalendarEntity entity = find(id);
        if (req.getName() != null) entity.setName(req.getName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getWorkdayMode() != null) entity.setWorkdayMode(req.getWorkdayMode());
        if (req.getTimezone() != null) entity.setTimezone(req.getTimezone());
        if (req.getWeekWorkdays() != null) entity.setWeekWorkdays(req.getWeekWorkdays());
        CalendarEntity saved = calendarRepository.save(entity);
        saveCut(id, req.getCutHour(), req.getCutMinute());
        return toRes(saved);
    }

    @Transactional
    public void delete(String id) {
        cutTimeRepository.deleteByCalendarId(id);
        // remove day overrides for this calendar
        calendarDayRepository.findByCalendarIdAndBizDateBetween(id, "0000-01-01", "9999-12-31")
                .forEach(calendarDayRepository::delete);
        calendarRepository.deleteById(id);
    }

    // ---- Days & cut ----

    @Transactional
    public void setDays(String calendarId, List<CalendarDaySaveItem> days) {
        find(calendarId);
        if (days == null) return;
        for (CalendarDaySaveItem item : days) {
            LocalDate parsed;
            try {
                parsed = LocalDate.parse(item.getDate());
            } catch (Exception e) {
                throw new BusinessException("Invalid date: " + item.getDate());
            }
            boolean workday = item.getWorkday() == null || item.getWorkday();
            if (workday) {
                // remove override (back to default behavior)
                calendarDayRepository.deleteByCalendarIdAndBizDate(calendarId, parsed.toString());
            } else {
                CalendarDayEntity d = calendarDayRepository
                        .findByCalendarIdAndBizDate(calendarId, parsed.toString())
                        .orElse(CalendarDayEntity.builder().calendarId(calendarId).build());
                d.setBizDate(parsed.toString());
                d.setWorkday(false);
                d.setDayType("HOLIDAY");
                d.setRemark(item.getRemark());
                calendarDayRepository.save(d);
            }
        }
    }

    @Transactional
    public void setCut(String calendarId, Integer hour, Integer minute) {
        find(calendarId);
        saveCut(calendarId, hour, minute);
    }

    // ---- Preview ----

    @Transactional(readOnly = true)
    public List<CalendarDayView> preview(String calendarId, LocalDate from, LocalDate to) {
        find(calendarId);
        CalendarEntity cal = find(calendarId);
        List<CalendarDayView> result = new ArrayList<>();
        Map<String, CalendarDayEntity> overrides = calendarDayRepository
                .findByCalendarIdAndBizDateBetween(calendarId, from.toString(), to.toString()).stream()
                .collect(Collectors.toMap(CalendarDayEntity::getBizDate, d -> d));
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            CalendarDayView v = new CalendarDayView();
            v.setDate(d.toString());
            CalendarDayEntity ov = overrides.get(d.toString());
            boolean workday = bizDateCalculator.isWorkday(calendarId, d);
            v.setWorkday(workday);
            if (ov != null) v.setRemark(ov.getRemark());
            if (!workday) {
                v.setDayType("WEEK".equalsIgnoreCase(cal.getWorkdayMode()) ? "WEEKEND" : "HOLIDAY");
            } else {
                v.setDayType("WORKDAY");
            }
            result.add(v);
        }
        return result;
    }

    private void saveCut(String calendarId, Integer hour, Integer minute) {
        cutTimeRepository.findByCalendarId(calendarId).ifPresentOrElse(c -> {
            c.setCutHour(hour == null ? 0 : hour);
            c.setCutMinute(minute == null ? 0 : minute);
            cutTimeRepository.save(c);
        }, () -> cutTimeRepository.save(CalendarCutTimeEntity.builder()
                .calendarId(calendarId)
                .cutHour(hour == null ? 0 : hour)
                .cutMinute(minute == null ? 0 : minute)
                .build()));
    }

    private CalendarEntity find(String id) {
        return calendarRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Calendar not found: " + id));
    }

    private CalendarRes toRes(CalendarEntity e) {
        CalendarRes r = new CalendarRes();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setDescription(e.getDescription());
        r.setWorkdayMode(e.getWorkdayMode());
        r.setTimezone(e.getTimezone());
        r.setWeekWorkdays(e.getWeekWorkdays());
        cutTimeRepository.findByCalendarId(e.getId()).ifPresent(c -> {
            r.setCutHour(c.getCutHour());
            r.setCutMinute(c.getCutMinute());
        });
        r.setCreateTime(e.getCreatedAt() != null ? e.getCreatedAt().format(DTF) : null);
        return r;
    }
}