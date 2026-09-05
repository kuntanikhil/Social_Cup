package com.socialcup.redemption;

import java.time.OffsetDateTime;

public record RedemptionSessionResponse(
        Long sessionId,
        RedemptionCafeResponse cafe,
        RedemptionSessionDrinkResponse drink,
        Integer credits,
        String status,
        OffsetDateTime expiresAt,
        OffsetDateTime serverTime
) {
}
