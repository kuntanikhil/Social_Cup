package com.socialcup.barista;

public record ValidateRedemptionRequest(
        String qrToken,
        String backupCode
) {
}
