package com.socialcup.cafe;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CafeOpeningHoursRepository extends JpaRepository<CafeOpeningHours, Long> {

    List<CafeOpeningHours> findByCafeIdOrderByDayOfWeekAsc(Long cafeId);
}
