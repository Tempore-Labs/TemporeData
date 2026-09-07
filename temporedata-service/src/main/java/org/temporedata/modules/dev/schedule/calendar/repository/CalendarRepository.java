package org.temporedata.modules.dev.schedule.calendar.repository;

import org.temporedata.modules.dev.schedule.calendar.entity.CalendarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarRepository extends JpaRepository<CalendarEntity, String> {

    boolean existsByName(String name);
}