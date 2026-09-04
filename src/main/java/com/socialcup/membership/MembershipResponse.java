package com.socialcup.membership;

import java.time.OffsetDateTime;

public record MembershipResponse(
        SubscriptionStatus status,
        boolean isMember,
        int creditsRemaining,
        OffsetDateTime currentPeriodEnd,
        boolean cancelAtPeriodEnd
) {
}
