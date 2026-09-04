package com.socialcup.cafe;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CafeResponse(
        Long id,
        String name,
        String address,
        Long neighbourhoodId,
        String neighbourhoodName,
        BigDecimal latitude,
        BigDecimal longitude,
        String perkLine,
        BigDecimal payoutRatePerCredit,
        boolean featured,
        boolean active,
        String scanSlug,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
