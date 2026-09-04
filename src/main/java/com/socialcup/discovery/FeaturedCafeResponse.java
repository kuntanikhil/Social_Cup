package com.socialcup.discovery;

public record FeaturedCafeResponse(
        Long id,
        String name,
        String neighbourhood,
        String perkLine,
        Integer minimumCreditPrice
) {
}
