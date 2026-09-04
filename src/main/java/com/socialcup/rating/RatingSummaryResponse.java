package com.socialcup.rating;

import java.math.BigDecimal;

public record RatingSummaryResponse(
        BigDecimal averageRating,
        long ratingCount
) {

    public static RatingSummaryResponse empty() {
        return new RatingSummaryResponse(null, 0);
    }
}
