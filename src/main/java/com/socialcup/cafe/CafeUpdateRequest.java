package com.socialcup.cafe;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CafeUpdateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 500) String address,
        @NotNull @Positive Long neighbourhoodId,
        @DecimalMin("-90.000000") @DecimalMax("90.000000") @Digits(integer = 3, fraction = 6) BigDecimal latitude,
        @DecimalMin("-180.000000") @DecimalMax("180.000000") @Digits(integer = 3, fraction = 6) BigDecimal longitude,
        @Size(max = 500) String perkLine,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal payoutRatePerCredit,
        @NotNull Boolean featured,
        @NotNull Boolean active
) {
}
