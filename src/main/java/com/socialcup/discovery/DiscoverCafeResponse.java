package com.socialcup.discovery;

import java.math.BigDecimal;

public record DiscoverCafeResponse(
        Long id,
        String name,
        String neighbourhood,
        String address,
        String perkLine,
        boolean featured,
        Integer minimumCreditPrice,
        BigDecimal distanceKm,
        boolean preferenceMatch,
        BigDecimal averageRating,
        long ratingCount,
        boolean newCafe
) {
}
