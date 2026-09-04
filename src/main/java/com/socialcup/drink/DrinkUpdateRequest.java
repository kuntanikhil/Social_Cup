package com.socialcup.drink;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DrinkUpdateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull DrinkType type,
        String description,
        @Size(max = 500) String photoPath,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal retailPrice,
        @NotNull @Positive Integer creditPrice,
        @NotNull Boolean signature,
        @NotNull Boolean active
) {
}
