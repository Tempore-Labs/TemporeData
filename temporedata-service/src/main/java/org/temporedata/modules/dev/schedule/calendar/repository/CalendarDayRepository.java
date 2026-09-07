package org.temporedata.modules.dev.schedule.calendar.repository;

import org.temporedata.modules.dev.schedule.calendar.entity.CalendarDayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public interface CalendarDayRepository extends JpaRepository<CalendarDayEntity, String> {

    Optional<CalendarDayEntity> findByCalendarIdAndBizDate(String calendarId, String bizDate);

    List<CalendarDayEntity> findByCalendarIdAndBizDateBetween(String calendarId, String from, String to);

    void deleteByCalendarIdAndBizDate(String calendarId, String bizDate);

    default Map<LocalDate, CalendarDayEntity> dayMap(String calendarId, LocalDate from, LocalDate to) {
        return findByCalendarIdAndBizDateBetween(calendarId, from.toString(), to.toString()).stream()
                .collect(Collectors.toMap(d -> LocalDate.parse(d.getBizDate()), Function.identity()));
    }
}