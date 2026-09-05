package com.socialcup.redemption;

import java.time.OffsetDateTime;

public record CreateRedemptionSessionResponse(
        Long sessionId,
        RedemptionCafeResponse cafe,
        RedemptionDrinkResponse drink,
        Integer creditsBefore,
        Integer creditsAfter,
        String qrToken,
        String backupCode,
        OffsetDateTime expiresAt,
        Long expiresInSeconds,
        String status
) {
}
