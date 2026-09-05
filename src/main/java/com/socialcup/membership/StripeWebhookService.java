package com.socialcup.membership;

import com.socialcup.credit.CreditService;
import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import com.stripe.model.Event;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class StripeWebhookService {

    private static final String INVOICE_PAID = "invoice.paid";
    private static final String INVOICE_PAYMENT_SUCCEEDED = "invoice.payment_succeeded";
    private static final String INVOICE_PAYMENT_FAILED = "invoice.payment_failed";
    private static final String SUBSCRIPTION_CREATED = "customer.subscription.created";
    private static final String SUBSCRIPTION_UPDATED = "customer.subscription.updated";
    private static final String SUBSCRIPTION_DELETED = "customer.subscription.deleted";

    private final StripeEventRepository stripeEventRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final UserRepository userRepository;
    private final CreditService creditService;
    private final ObjectMapper objectMapper;

    public StripeWebhookService(
            StripeEventRepository stripeEventRepository,
            SubscriptionRepository subscriptionRepository,
            BillingCycleRepository billingCycleRepository,
            UserRepository userRepository,
            CreditService creditService,
            ObjectMapper objectMapper
    ) {
        this.stripeEventRepository = stripeEventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.billingCycleRepository = billingCycleRepository;
        this.userRepository = userRepository;
        this.creditService = creditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void process(Event event, String payload) {
        int claimed = stripeEventRepository.claim(event.getId(), event.getType());
        if (claimed == 0) {
            return;
        }

        JsonNode stripeObject = readStripeObject(payload);
        switch (event.getType()) {
            case INVOICE_PAID, INVOICE_PAYMENT_SUCCEEDED ->
                    processSuccessfulInvoice(stripeObject);
            case INVOICE_PAYMENT_FAILED -> processFailedInvoice(stripeObject);
            case SUBSCRIPTION_CREATED, SUBSCRIPTION_UPDATED ->
                    synchronizeSubscription(stripeObject, false);
            case SUBSCRIPTION_DELETED -> synchronizeSubscription(stripeObject, true);
            default -> {
                // The verified event is intentionally acknowledged but needs no MVP action.
            }
        }

        StripeEvent claimedEvent = stripeEventRepository
                .findByStripeEventId(event.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Claimed Stripe event could not be loaded"
                ));
        claimedEvent.markProcessed(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private void processSuccessfulInvoice(JsonNode invoice) {
        String invoiceId = text(invoice, "id");
        if (invoiceId == null) {
            throw new IllegalArgumentException("Stripe invoice ID is required");
        }
        Optional<Subscription> localSubscription = findAndLockSubscription(
                invoiceSubscriptionId(invoice),
                text(invoice, "customer")
        );
        if (localSubscription.isEmpty()) {
            return;
        }

        Subscription subscription = localSubscription.get();
        if (subscription.getStatus() == SubscriptionStatus.ENDED) {
            return;
        }
        StripePeriod period = invoicePeriod(invoice)
                .orElseGet(() -> existingPeriod(subscription));
        if (period == null || !period.end().isAfter(period.start())) {
            throw new IllegalStateException(
                    "A valid Stripe billing period is required to grant credits"
            );
        }

        subscription.activateFromSuccessfulPayment(period.start(), period.end());
        subscriptionRepository.save(subscription);

        if (!grantsMonthlyCredits(invoice)) {
            return;
        }

        if (billingCycleRepository.findByStripeReference(invoiceId).isPresent()) {
            return;
        }

        OffsetDateTime processedAt = OffsetDateTime.now(ZoneOffset.UTC);
        BillingCycle billingCycle = billingCycleRepository.save(
                BillingCycle.createProcessedStripe(
                        subscription,
                        period.start(),
                        period.end(),
                        invoiceId,
                        processedAt
                )
        );
        creditService.resetForSuccessfulCycle(subscription.getUser(), billingCycle);
    }

    private boolean grantsMonthlyCredits(JsonNode invoice) {
        String billingReason = text(invoice, "billing_reason");
        return "subscription_create".equals(billingReason)
                || "subscription_cycle".equals(billingReason)
                || "subscription".equals(billingReason);
    }

    private void processFailedInvoice(JsonNode invoice) {
        String invoiceId = text(invoice, "id");
        Optional<Subscription> localSubscription = findAndLockSubscription(
                invoiceSubscriptionId(invoice),
                text(invoice, "customer")
        );
        if (localSubscription.isEmpty()
                || (invoiceId != null
                && billingCycleRepository.findByStripeReference(invoiceId).isPresent())
                || localSubscription.get().getStatus() == SubscriptionStatus.ENDED) {
            return;
        }
        Subscription subscription = localSubscription.get();
        subscription.markPaymentFailed();
        subscriptionRepository.save(subscription);
    }

    private void synchronizeSubscription(
            JsonNode stripeSubscription,
            boolean deleted
    ) {
        String stripeSubscriptionId = text(stripeSubscription, "id");
        String customerId = text(stripeSubscription, "customer");
        findAndLockSubscription(stripeSubscriptionId, customerId)
                .ifPresent(subscription -> {
                    if (subscription.getStatus() == SubscriptionStatus.ENDED && !deleted) {
                        return;
                    }
                    boolean cancelAtPeriodEnd = stripeSubscription
                            .path("cancel_at_period_end")
                            .asBoolean(false);
                    SubscriptionStatus status = mapStripeStatus(
                            text(stripeSubscription, "status"),
                            cancelAtPeriodEnd,
                            deleted
                    );
                    if (status == SubscriptionStatus.INCOMPLETE
                            && subscription.getStatus().isMember()) {
                        status = subscription.getStatus();
                    }
                    StripePeriod period = subscriptionPeriod(stripeSubscription)
                            .orElse(null);
                    subscription.synchronizeFromStripe(
                            status,
                            period == null ? null : period.start(),
                            period == null ? null : period.end(),
                            cancelAtPeriodEnd
                    );
                    subscriptionRepository.save(subscription);
                });
    }

    private Optional<Subscription> findAndLockSubscription(
            String stripeSubscriptionId,
            String customerId
    ) {
        Optional<Long> userId = Optional.empty();
        if (stripeSubscriptionId != null) {
            userId = subscriptionRepository.findUserIdByStripeSubscriptionId(
                    stripeSubscriptionId
            );
        }
        if (userId.isEmpty() && customerId != null) {
            userId = subscriptionRepository.findUserIdByStripeCustomerId(customerId);
        }
        if (userId.isEmpty()) {
            return Optional.empty();
        }

        User user = userRepository.findByIdForUpdate(userId.get())
                .orElseThrow(() -> new IllegalStateException(
                        "Stripe subscription user no longer exists"
                ));
        Optional<Subscription> subscription = subscriptionRepository.findByUserId(
                user.getId()
        );
        if (subscription.isEmpty()) {
            return Optional.empty();
        }

        String currentStripeSubscriptionId = subscription.get()
                .getStripeSubscriptionId();
        if (stripeSubscriptionId != null
                && currentStripeSubscriptionId != null
                && !stripeSubscriptionId.equals(currentStripeSubscriptionId)) {
            return Optional.empty();
        }
        return subscription;
    }

    private JsonNode readStripeObject(String payload) {
        try {
            JsonNode stripeObject = objectMapper.readTree(payload)
                    .path("data")
                    .path("object");
            if (stripeObject.isMissingNode() || !stripeObject.isObject()) {
                throw new IllegalArgumentException("Invalid Stripe event payload");
            }
            return stripeObject;
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid Stripe event payload", exception);
        }
    }

    private String invoiceSubscriptionId(JsonNode invoice) {
        String currentApiValue = text(
                invoice.path("parent").path("subscription_details"),
                "subscription"
        );
        return currentApiValue != null
                ? currentApiValue
                : text(invoice, "subscription");
    }

    private Optional<StripePeriod> invoicePeriod(JsonNode invoice) {
        JsonNode lines = invoice.path("lines").path("data");
        if (lines.isArray()) {
            Optional<StripePeriod> linePeriod = StreamSupport
                    .stream(lines.spliterator(), false)
                    .map(line -> period(
                            line.path("period").path("start"),
                            line.path("period").path("end")
                    ))
                    .flatMap(Optional::stream)
                    .max(Comparator.comparing(period -> Duration.between(
                            period.start(),
                            period.end()
                    )));
            if (linePeriod.isPresent()) {
                return linePeriod;
            }
        }
        return period(invoice.path("period_start"), invoice.path("period_end"));
    }

    private Optional<StripePeriod> subscriptionPeriod(JsonNode subscription) {
        JsonNode items = subscription.path("items").path("data");
        if (items.isArray()) {
            Optional<StripePeriod> itemPeriod = StreamSupport
                    .stream(items.spliterator(), false)
                    .map(item -> period(
                            item.path("current_period_start"),
                            item.path("current_period_end")
                    ))
                    .flatMap(Optional::stream)
                    .findFirst();
            if (itemPeriod.isPresent()) {
                return itemPeriod;
            }
        }
        return period(
                subscription.path("current_period_start"),
                subscription.path("current_period_end")
        );
    }

    private Optional<StripePeriod> period(JsonNode start, JsonNode end) {
        if (!start.canConvertToLong() || !end.canConvertToLong()) {
            return Optional.empty();
        }
        OffsetDateTime periodStart = OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(start.asLong()),
                ZoneOffset.UTC
        );
        OffsetDateTime periodEnd = OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(end.asLong()),
                ZoneOffset.UTC
        );
        if (!periodEnd.isAfter(periodStart)) {
            return Optional.empty();
        }
        return Optional.of(new StripePeriod(periodStart, periodEnd));
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

    private SubscriptionStatus mapStripeStatus(
            String stripeStatus,
            boolean cancelAtPeriodEnd,
            boolean deleted
    ) {
        if (deleted || "canceled".equals(stripeStatus)
                || "incomplete_expired".equals(stripeStatus)) {
            return SubscriptionStatus.ENDED;
        }
        if ("past_due".equals(stripeStatus)
                || "unpaid".equals(stripeStatus)
                || "paused".equals(stripeStatus)) {
            return SubscriptionStatus.PAYMENT_FAILED;
        }
        if ("active".equals(stripeStatus)) {
            return cancelAtPeriodEnd
                    ? SubscriptionStatus.CANCEL_AT_PERIOD_END
                    : SubscriptionStatus.ACTIVE;
        }
        return SubscriptionStatus.INCOMPLETE;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isString() && !value.asString().isBlank()
                ? value.asString()
                : null;
    }

    private record StripePeriod(OffsetDateTime start, OffsetDateTime end) {
    }
}
