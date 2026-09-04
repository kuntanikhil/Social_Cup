package com.socialcup.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProfileUpdateRequest(
        @NotBlank @Size(max = 100) String displayName,
        @NotNull @Positive Long homeNeighbourhoodId,
        @NotNull List<@NotNull @Positive Long> coffeePreferenceIds
) {
}
