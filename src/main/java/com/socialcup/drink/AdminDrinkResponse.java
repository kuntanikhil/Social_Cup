package com.socialcup.drink;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminDrinkResponse(
        Long id,
        Long cafeId,
        String name,
        DrinkType type,
        String description,
        String photoPath,
        BigDecimal retailPrice,
        Integer creditPrice,
        boolean signature,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
