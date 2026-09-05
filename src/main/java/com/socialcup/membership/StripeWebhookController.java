package com.socialcup.membership;

import com.stripe.model.Event;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StripeWebhookController {

    private final StripeGateway stripeGateway;
    private final StripeWebhookService stripeWebhookService;

    public StripeWebhookController(
            StripeGateway stripeGateway,
            StripeWebhookService stripeWebhookService
    ) {
        this.stripeGateway = stripeGateway;
        this.stripeWebhookService = stripeWebhookService;
    }

    @PostMapping("/api/webhooks/stripe")
    public ResponseEntity<Void> receive(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature
    ) {
        Event event = stripeGateway.verifyWebhook(payload, signature);
        stripeWebhookService.process(event, payload);
        return ResponseEntity.ok().build();
    }
}
