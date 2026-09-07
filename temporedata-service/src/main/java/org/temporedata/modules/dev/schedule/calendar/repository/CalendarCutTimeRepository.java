package org.temporedata.modules.dev.schedule.calendar.repository;

import org.temporedata.modules.dev.schedule.calendar.entity.CalendarCutTimeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CalendarCutTimeRepository extends JpaRepository<CalendarCutTimeEntity, String> {

    Optional<CalendarCutTimeEntity> findByCalendarId(String calendarId);

    void deleteByCalendarId(String calendarId);
}