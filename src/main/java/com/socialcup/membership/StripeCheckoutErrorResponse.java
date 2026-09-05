package com.socialcup.membership;

public record StripeCheckoutErrorResponse(
        String error,
        String stage,
        String stripeCode,
        String stripeRequestId
) {
}
