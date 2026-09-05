package com.socialcup.membership;

public record StripeCheckoutResponse(
        String subscriptionId,
        String clientSecret,
        String ephemeralKey,
        String customerId
) {
}
