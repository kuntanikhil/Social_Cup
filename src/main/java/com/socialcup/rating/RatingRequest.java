package com.socialcup.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RatingRequest(
        @NotNull @Min(1) @Max(5) Integer stars,
        @Size(max = 140) String note
) {
}
