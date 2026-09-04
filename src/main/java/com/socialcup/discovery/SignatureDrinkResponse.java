package com.socialcup.discovery;

import com.socialcup.drink.DrinkType;

public record SignatureDrinkResponse(
        Long id,
        String name,
        DrinkType type,
        Integer creditPrice,
        String photoPath,
        SignatureDrinkCafeResponse cafe
) {
}
