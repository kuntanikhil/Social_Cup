package com.socialcup.barista;

import java.time.OffsetDateTime;

public record BaristaTodayRedemptionResponse(
        Long redemptionId,
        String memberFirstName,
        String drinkName,
        Integer creditsSpent,
        OffsetDateTime redeemedAt
) {
}
