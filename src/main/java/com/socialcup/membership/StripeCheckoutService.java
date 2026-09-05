package com.socialcup.membership;

import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.EphemeralKey;
import com.stripe.model.Invoice;
import com.stripe.model.SubscriptionItem;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class StripeCheckoutService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final StripeGateway stripeGateway;

    public StripeCheckoutService(
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            StripeGateway stripeGateway
    ) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.stripeGateway = stripeGateway;
    }

    @Transactional
    public StripeCheckoutResponse createCheckout(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        com.socialcup.membership.Subscription localSubscription =
                subscriptionRepository.findByUserId(userId)
                        .orElseGet(() -> subscriptionRepository.save(
                                com.socialcup.membership.Subscription
                                        .createForStripeCheckout(user)
                        ));

        if (hasCurrentStripeCheckout(localSubscription)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A Stripe subscription already exists for this membership"
            );
        }

        String stripeStage = "customer";
        try {
            String customerId = localSubscription.getStripeCustomerId();
            if (customerId == null || customerId.isBlank()) {
                customerId = stripeGateway.createCustomer(user).getId();
                localSubscription.setStripeCustomerId(customerId);
                subscriptionRepository.save(localSubscription);
            }

            stripeStage = "ephemeral_key";
            EphemeralKey ephemeralKey = stripeGateway.createEphemeralKey(customerId);
            stripeStage = "subscription";
            com.stripe.model.Subscription stripeSubscription =
                    stripeGateway.createIncompleteSubscription(customerId, userId);
            Invoice latestInvoice = stripeSubscription.getLatestInvoiceObject();
            if (latestInvoice == null
                    || latestInvoice.getConfirmationSecret() == null
                    || latestInvoice.getConfirmationSecret().getClientSecret() == null) {
                throw new StripeCheckoutException(
                        "subscription_response",
                        "missing_client_secret",
                        null
                );
            }

            StripePeriod period = currentPeriod(stripeSubscription);
            localSubscription.beginStripeCheckout(
                    stripeSubscription.getId(),
                    period == null ? null : period.start(),
                    period == null ? null : period.end(),
                    Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd())
            );
            subscriptionRepository.save(localSubscription);

            return new StripeCheckoutResponse(
                    stripeSubscription.getId(),
                    latestInvoice.getConfirmationSecret().getClientSecret(),
                    ephemeralKey.getSecret(),
                    customerId
            );
        } catch (StripeException exception) {
            throw new StripeCheckoutException(
                    stripeStage,
                    exception.getCode(),
                    exception.getRequestId()
            );
        }
    }

    private boolean hasCurrentStripeCheckout(Subscription subscription) {
        return subscription.getStripeSubscriptionId() != null
                && !subscription.getStripeSubscriptionId().isBlank()
                && subscription.getStatus() != SubscriptionStatus.ENDED;
    }

    private StripePeriod currentPeriod(
            com.stripe.model.Subscription stripeSubscription
    ) {
        if (stripeSubscription.getItems() == null
                || stripeSubscription.getItems().getData() == null) {
            return null;
        }
        return stripeSubscription.getItems().getData().stream()
                .map(this::toPeriod)
                .filter(period -> period != null && period.end().isAfter(period.start()))
                .findFirst()
                .orElse(null);
    }

    private StripePeriod toPeriod(SubscriptionItem item) {
        if (item.getCurrentPeriodStart() == null
                || item.getCurrentPeriodEnd() == null) {
            return null;
        }
        return new StripePeriod(
                OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(item.getCurrentPeriodStart()),
                        ZoneOffset.UTC
                ),
                OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(item.getCurrentPeriodEnd()),
                        ZoneOffset.UTC
                )
        );
    }

    private record StripePeriod(OffsetDateTime start, OffsetDateTime end) {
    }
}
