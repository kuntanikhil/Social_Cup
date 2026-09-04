package com.socialcup.drink;

import java.math.BigDecimal;

public record DrinkResponse(
        Long id,
        String name,
        DrinkType type,
        String description,
        String photoPath,
        BigDecimal retailPrice,
        Integer creditPrice,
        boolean signature
) {
}
