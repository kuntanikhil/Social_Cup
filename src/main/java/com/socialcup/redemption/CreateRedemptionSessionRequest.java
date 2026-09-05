package com.socialcup.redemption;

import jakarta.validation.constraints.NotNull;

public record CreateRedemptionSessionRequest(
        @NotNull Long drinkId
) {
}
