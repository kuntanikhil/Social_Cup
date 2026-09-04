package com.socialcup.discovery;

import java.math.BigDecimal;

public record FeaturedCafeResponse(
        Long id,
        String name,
        String neighbourhood,
        String perkLine,
        Integer minimumCreditPrice,
        BigDecimal averageRating,
        long ratingCount,
        boolean newCafe
) {
}
