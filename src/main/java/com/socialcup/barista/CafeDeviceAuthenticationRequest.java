package com.socialcup.barista;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CafeDeviceAuthenticationRequest(
        @NotNull Long cafeId,
        @NotBlank @Size(max = 72) String pin
) {
}
