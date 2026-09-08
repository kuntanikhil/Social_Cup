package com.socialcup.membership;

import com.socialcup.user.User;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.EphemeralKey;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.EphemeralKeyCreateParams;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StripeGateway {

    private static final Set<String> RESUMABLE_SUBSCRIPTION_STATUSES =
            Set.of("incomplete", "past_due");

    private final StripeProperties properties;
    private final RequestOptions requestOptions;

    public StripeGateway(
            StripeProperties properties,
            RequestOptions requestOptions
    ) {
        this.properties = properties;
        this.requestOptions = requestOptions;
    }

    public Customer createCustomer(User user) throws StripeException {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("email", user.getEmail());
        parameters.put("name", user.getDisplayName());
        parameters.put("metadata", Map.of(
                "socialcup_user_id",
                user.getId().toString()
        ));
        return Customer.create(parameters, requestOptions);
    }

    public EphemeralKey createEphemeralKey(String customerId) throws StripeException {
        EphemeralKeyCreateParams parameters = EphemeralKeyCreateParams.builder()
                .setCustomer(customerId)
                .setStripeVersion(Stripe.API_VERSION)
                .build();
        return EphemeralKey.create(parameters, requestOptions);
    }

    public Subscription createIncompleteSubscription(
            String customerId,
            Long userId
    ) throws StripeException {
        String priceId = requireConfigured(properties.getPriceId(), "Stripe price ID");

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("customer", customerId);
        parameters.put("items", List.of(Map.of("price", priceId)));
        parameters.put("payment_behavior", "default_incomplete");
        parameters.put("payment_settings", Map.of(
                "save_default_payment_method",
                "on_subscription"
        ));
        parameters.put("billing_mode", Map.of("type", "flexible"));
        parameters.put("expand", List.of("latest_invoice.confirmation_secret"));
        parameters.put("metadata", Map.of(
                "socialcup_user_id",
                userId.toString()
        ));
        return Subscription.create(parameters, requestOptions);
    }

    public Subscription retrieveSubscription(String subscriptionId)
            throws StripeException {
        return Subscription.retrieve(
                subscriptionId,
                Map.of("expand", List.of("latest_invoice.confirmation_secret")),
                requestOptions
        );
    }

    /**
     * Rebuilds PaymentSheet credentials for an existing Stripe subscription.
     * This never creates a second subscription; only the short-lived customer
     * ephemeral key is refreshed.
     */
    public StripeCheckoutResponse resumeSubscriptionCheckout(
            String subscriptionId,
            String expectedCustomerId
    ) throws StripeException {
        Subscription stripeSubscription = retrieveSubscription(subscriptionId);
        if (!RESUMABLE_SUBSCRIPTION_STATUSES.contains(stripeSubscription.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The existing Stripe subscription is not in a resumable payment state"
            );
        }

        String actualCustomerId = stripeSubscription.getCustomer();
        if (actualCustomerId == null || !actualCustomerId.equals(expectedCustomerId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The existing Stripe subscription customer does not match the local membership"
            );
        }

        Invoice latestInvoice = stripeSubscription.getLatestInvoiceObject();
        if (latestInvoice == null
                && stripeSubscription.getLatestInvoice() != null
                && !stripeSubscription.getLatestInvoice().isBlank()) {
            latestInvoice = retrieveInvoice(stripeSubscription.getLatestInvoice());
        }
        if (latestInvoice == null
                || latestInvoice.getConfirmationSecret() == null
                || latestInvoice.getConfirmationSecret().getClientSecret() == null
                || latestInvoice.getConfirmationSecret().getClientSecret().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The existing Stripe subscription no longer has a resumable payment"
            );
        }

        EphemeralKey ephemeralKey = createEphemeralKey(expectedCustomerId);
        if (ephemeralKey.getSecret() == null || ephemeralKey.getSecret().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Stripe did not return customer session credentials"
            );
        }

        return new StripeCheckoutResponse(
                stripeSubscription.getId(),
                latestInvoice.getConfirmationSecret().getClientSecret(),
                ephemeralKey.getSecret(),
                expectedCustomerId
        );
    }

    public Invoice retrieveInvoice(String invoiceId) throws StripeException {
        return Invoice.retrieve(
                invoiceId,
                Map.of("expand", List.of("confirmation_secret")),
                requestOptions
        );
    }

    public Event verifyWebhook(String payload, String signatureHeader) {
        String webhookSecret = properties.getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Stripe webhook secret is not configured"
            );
        }
        try {
            return Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Stripe webhook signature"
            );
        }
    }

    private String requireConfigured(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    propertyName + " is not configured"
            );
        }
        return value;
    }
}
