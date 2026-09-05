package com.socialcup.membership;

import com.socialcup.credit.CreditService;
import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.SubscriptionItem;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Optional;

@Service
public class MembershipService {

    private final SubscriptionRepository subscriptionRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final CreditService creditService;
    private final UserRepository userRepository;
    private final StripeGateway stripeGateway;

    public MembershipService(
            SubscriptionRepository subscriptionRepository,
            BillingCycleRepository billingCycleRepository,
            CreditService creditService,
            UserRepository userRepository,
            StripeGateway stripeGateway
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.billingCycleRepository = billingCycleRepository;
        this.creditService = creditService;
        this.userRepository = userRepository;
        this.stripeGateway = stripeGateway;
    }

    @Transactional
    public MembershipResponse getMembership(Long userId) {
        User user = getUserForUpdate(userId);
        int creditsRemaining = creditService.getBalance(user);
        return subscriptionRepository.findByUserId(userId)
                .map(subscription -> toResponse(subscription, creditsRemaining))
                .orElseGet(() -> new MembershipResponse(
                        SubscriptionStatus.NONE,
                        false,
                        creditsRemaining,
                        null,
                        false
                ));
    }

    @Transactional
    public MembershipResponse demoActivate(Long userId) {
        User user = getUserForUpdate(userId);
        OffsetDateTime periodStart = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime periodEnd = periodStart.plusMonths(1);

        Optional<Subscription> existingSubscription = subscriptionRepository.findByUserId(userId);
        Subscription subscription;
        if (existingSubscription.isPresent()) {
            subscription = existingSubscription.get();
            subscription.activateDemo(periodStart, periodEnd);
        } else {
            subscription = Subscription.createDemo(user, periodStart, periodEnd);
        }
        subscription = subscriptionRepository.save(subscription);

        BillingCycle billingCycle = billingCycleRepository.save(
                BillingCycle.createProcessedDemo(
                        subscription,
                        periodStart,
                        periodEnd,
                        periodStart
                )
        );
        int creditsRemaining = creditService.resetForSuccessfulCycle(
                user,
                billingCycle
        );

        return toResponse(subscription, creditsRemaining);
    }

    @Transactional
    public MembershipResponse reconcile(Long userId) {
        User user = getUserForUpdate(userId);
        Subscription localSubscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Membership subscription not found"
                ));
        String stripeSubscriptionId = localSubscription.getStripeSubscriptionId();
        if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Membership has no Stripe subscription"
            );
        }

        com.stripe.model.Subscription stripeSubscription;
        try {
            stripeSubscription = stripeGateway.retrieveSubscription(
                    stripeSubscriptionId
            );
        } catch (StripeException exception) {
            throw stripeReconciliationFailure(
                    "reconcile_subscription",
                    exception
            );
        }

        Invoice latestInvoice = stripeSubscription.getLatestInvoiceObject();
        if (latestInvoice == null
                && stripeSubscription.getLatestInvoice() != null) {
            try {
                latestInvoice = stripeGateway.retrieveInvoice(
                        stripeSubscription.getLatestInvoice()
                );
            } catch (StripeException exception) {
                throw stripeReconciliationFailure("reconcile_invoice", exception);
            }
        }

        Invoice reconciledInvoice = latestInvoice;
        boolean stripeActive = "active".equals(stripeSubscription.getStatus());
        boolean latestInvoicePaid = reconciledInvoice != null
                && "paid".equals(reconciledInvoice.getStatus());
        boolean cancelAtPeriodEnd = Boolean.TRUE.equals(
                stripeSubscription.getCancelAtPeriodEnd()
        );
        SubscriptionStatus synchronizedStatus = reconciliationStatus(
                stripeSubscription.getStatus(),
                stripeActive && latestInvoicePaid,
                cancelAtPeriodEnd
        );
        StripePeriod period = subscriptionPeriod(stripeSubscription)
                .or(() -> invoicePeriod(reconciledInvoice))
                .orElseGet(() -> existingPeriod(localSubscription));

        if (stripeActive && latestInvoicePaid
                && (period == null || !period.end().isAfter(period.start()))) {
            throw new StripeCheckoutException(
                    "Stripe reconciliation failed",
                    "reconcile_period",
                    "missing_billing_period",
                    null
            );
        }

        localSubscription.synchronizeFromStripe(
                synchronizedStatus,
                period == null ? null : period.start(),
                period == null ? null : period.end(),
                cancelAtPeriodEnd
        );
        subscriptionRepository.save(localSubscription);

        int creditsRemaining;
        if (stripeActive && latestInvoicePaid) {
            String invoiceId = reconciledInvoice.getId();
            if (invoiceId == null || invoiceId.isBlank()) {
                throw new StripeCheckoutException(
                        "Stripe reconciliation failed",
                        "reconcile_invoice",
                        "missing_invoice_id",
                        null
                );
            }
            Optional<BillingCycle> existingCycle = billingCycleRepository
                    .findByStripeReference(invoiceId);
            if (existingCycle.isPresent()) {
                creditsRemaining = creditService.getBalance(user);
            } else {
                OffsetDateTime processedAt = OffsetDateTime.now(ZoneOffset.UTC);
                BillingCycle billingCycle = billingCycleRepository.save(
                        BillingCycle.createProcessedStripe(
                                localSubscription,
                                period.start(),
                                period.end(),
                                invoiceId,
                                processedAt
                        )
                );
                creditsRemaining = creditService.resetForSuccessfulCycle(
                        user,
                        billingCycle
                );
            }
        } else {
            creditsRemaining = creditService.getBalance(user);
        }

        return toResponse(localSubscription, creditsRemaining);
    }

    private StripeCheckoutException stripeReconciliationFailure(
            String stage,
            StripeException exception
    ) {
        return new StripeCheckoutException(
                "Stripe reconciliation failed",
                stage,
                exception.getCode(),
                exception.getRequestId()
        );
    }

    private SubscriptionStatus reconciliationStatus(
            String stripeStatus,
            boolean activeAndPaid,
            boolean cancelAtPeriodEnd
    ) {
        if (activeAndPaid) {
            return cancelAtPeriodEnd
                    ? SubscriptionStatus.CANCEL_AT_PERIOD_END
                    : SubscriptionStatus.ACTIVE;
        }
        return switch (stripeStatus) {
            case "canceled", "incomplete_expired" -> SubscriptionStatus.ENDED;
            case "past_due", "unpaid", "paused", "active" ->
                    SubscriptionStatus.PAYMENT_FAILED;
            default -> SubscriptionStatus.INCOMPLETE;
        };
    }

    private Optional<StripePeriod> subscriptionPeriod(
            com.stripe.model.Subscription stripeSubscription
    ) {
        if (stripeSubscription.getItems() == null
                || stripeSubscription.getItems().getData() == null) {
            return Optional.empty();
        }
        return stripeSubscription.getItems().getData().stream()
                .map(this::period)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<StripePeriod> invoicePeriod(Invoice invoice) {
        if (invoice == null || invoice.getLines() == null
                || invoice.getLines().getData() == null) {
            return Optional.empty();
        }
        return invoice.getLines().getData().stream()
                .map(this::period)
                .flatMap(Optional::stream)
                .max(Comparator.comparing(period -> Duration.between(
                        period.start(),
                        period.end()
                )));
    }

    private Optional<StripePeriod> period(SubscriptionItem item) {
        return period(item.getCurrentPeriodStart(), item.getCurrentPeriodEnd());
    }

    private Optional<StripePeriod> period(InvoiceLineItem lineItem) {
        if (lineItem.getPeriod() == null) {
            return Optional.empty();
        }
        return period(
                lineItem.getPeriod().getStart(),
                lineItem.getPeriod().getEnd()
        );
    }

    private Optional<StripePeriod> period(Long start, Long end) {
        if (start == null || end == null || end <= start) {
            return Optional.empty();
        }
        return Optional.of(new StripePeriod(
                OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(start),
                        ZoneOffset.UTC
                ),
                OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(end),
                        ZoneOffset.UTC
                )
        ));
    }

    private StripePeriod existingPeriod(Subscription subscription) {
        if (subscription.getCurrentPeriodStart() == null
                || subscription.getCurrentPeriodEnd() == null) {
            return null;
        }
        return new StripePeriod(
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd()
        );
    }

    private User getUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    private MembershipResponse toResponse(
            Subscription subscription,
            int creditsRemaining
    ) {
        return new MembershipResponse(
                subscription.getStatus(),
                subscription.getStatus().isMember(),
                creditsRemaining,
                subscription.getCurrentPeriodEnd(),
                subscription.isCancelAtPeriodEnd()
        );
    }

    private record StripePeriod(OffsetDateTime start, OffsetDateTime end) {
    }
}
