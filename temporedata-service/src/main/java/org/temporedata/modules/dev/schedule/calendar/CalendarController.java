package org.temporedata.modules.dev.schedule.calendar;

import lombok.RequiredArgsConstructor;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.api.dev.schedule.CalendarDaySaveItem;
import org.temporedata.api.dev.schedule.CalendarDayView;
import org.temporedata.api.dev.schedule.CalendarReq;
import org.temporedata.api.dev.schedule.CalendarRes;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * P1-4 business calendar REST API (custom calendars + day-cut).
 */
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping
    public BaseResponse<List<CalendarRes>> list() {
        return BaseResponse.success(calendarService.list());
    }

    @GetMapping("/{id}")
    public BaseResponse<CalendarRes> get(@PathVariable String id) {
        return BaseResponse.success(calendarService.get(id));
    }

    @PostMapping
    public BaseResponse<CalendarRes> create(@RequestBody CalendarReq req) {
        return BaseResponse.success(calendarService.create(req));
    }

    @PutMapping("/{id}")
    public BaseResponse<CalendarRes> update(@PathVariable String id, @RequestBody CalendarReq req) {
        return BaseResponse.success(calendarService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        calendarService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/days")
    public BaseResponse<Void> setDays(@PathVariable String id, @RequestBody List<CalendarDaySaveItem> days) {
        calendarService.setDays(id, days);
        return BaseResponse.success();
    }

    @PutMapping("/{id}/cut")
    public BaseResponse<Void> setCut(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        calendarService.setCut(id, body.getOrDefault("cutHour", 0), body.getOrDefault("cutMinute", 0));
        return BaseResponse.success();
    }

    @GetMapping("/{id}/preview")
    public BaseResponse<List<CalendarDayView>> preview(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return BaseResponse.success(calendarService.preview(id, from, to));
    }
}