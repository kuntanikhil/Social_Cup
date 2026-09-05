package com.socialcup.admin;

import com.socialcup.redemption.RedemptionStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminRedemptionResponse(
        Long redemptionId,
        OffsetDateTime redeemedAt,
        String memberName,
        Long cafeId,
        String cafeName,
        Long drinkId,
        String drinkName,
        Integer creditsSpent,
        RedemptionStatus status,
        BigDecimal payoutAmount
) {
}
