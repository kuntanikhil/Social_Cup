package com.socialcup.membership;

public enum SubscriptionStatus {
    NONE,
    ACTIVE,
    PAYMENT_FAILED,
    CANCEL_AT_PERIOD_END,
    ENDED;

    public boolean isMember() {
        return this == ACTIVE || this == CANCEL_AT_PERIOD_END;
    }
}
