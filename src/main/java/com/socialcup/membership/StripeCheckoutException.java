package com.socialcup.membership;

public class StripeCheckoutException extends RuntimeException {

    private final String stage;
    private final String stripeCode;
    private final String stripeRequestId;

    public StripeCheckoutException(
            String stage,
            String stripeCode,
            String stripeRequestId
    ) {
        this("Stripe checkout failed", stage, stripeCode, stripeRequestId);
    }

    public StripeCheckoutException(
            String error,
            String stage,
            String stripeCode,
            String stripeRequestId
    ) {
        super(error);
        this.stage = stage;
        this.stripeCode = stripeCode;
        this.stripeRequestId = stripeRequestId;
    }

    public String getStage() {
        return stage;
    }

    public String getStripeCode() {
        return stripeCode;
    }

    public String getStripeRequestId() {
        return stripeRequestId;
    }
}
