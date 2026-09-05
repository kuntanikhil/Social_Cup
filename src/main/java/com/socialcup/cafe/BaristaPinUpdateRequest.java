package com.socialcup.cafe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BaristaPinUpdateRequest(
        @NotBlank
        @Pattern(
                regexp = "\\d{4,6}",
                message = "PIN must contain 4 to 6 numeric digits"
        )
        String pin
) {
}
