package com.socialcup.barista;

import java.time.OffsetDateTime;

public record CafeDeviceAuthenticationResponse(
        Long cafeId,
        String cafeName,
        String deviceToken,
        OffsetDateTime expiresAt
) {
}
