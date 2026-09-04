package com.socialcup.cafe;

import java.time.LocalTime;

public record CafeOpeningHoursResponse(
        Long id,
        Short dayOfWeek,
        LocalTime opensAt,
        LocalTime closesAt,
        boolean closed
) {
}
