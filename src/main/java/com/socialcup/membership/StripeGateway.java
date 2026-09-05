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

@Service
public class StripeGateway {

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
                Map.of("expand", List.of("latest_invoice")),
                requestOptions
        );
    }

    public Invoice retrieveInvoice(String invoiceId) throws StripeException {
        return Invoice.retrieve(invoiceId, requestOptions);
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
