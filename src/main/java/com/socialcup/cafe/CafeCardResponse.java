package com.socialcup.cafe;

import java.math.BigDecimal;

public record CafeCardResponse(
        Long id,
        String name,
        String neighbourhoodName,
        String address,
        String perkLine,
        boolean featured,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer minimumCreditPrice
) {
}
