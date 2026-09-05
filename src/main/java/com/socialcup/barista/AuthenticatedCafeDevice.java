package com.socialcup.barista;

public record AuthenticatedCafeDevice(
        Long deviceId,
        Long cafeId
) {
}
