package com.socialcup.barista;

public record BaristaRedemptionResponse(
        String result,
        Long redemptionId,
        BaristaMemberResponse member,
        BaristaDrinkResponse drink,
        Integer creditsDeducted,
        Integer creditsRemaining
) {
}
