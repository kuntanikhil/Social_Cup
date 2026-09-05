package com.socialcup.membership;

import com.socialcup.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", unique = true, length = 255)
    private String stripeSubscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionStatus status;

    @Column(name = "current_period_start")
    private OffsetDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private OffsetDateTime currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Subscription() {
    }

    private Subscription(
            User user,
            SubscriptionStatus status,
            OffsetDateTime currentPeriodStart,
            OffsetDateTime currentPeriodEnd
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.user = user;
        this.status = status;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.cancelAtPeriodEnd = false;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Subscription createDemo(
            User user,
            OffsetDateTime currentPeriodStart,
            OffsetDateTime currentPeriodEnd
    ) {
        return new Subscription(
                user,
                SubscriptionStatus.ACTIVE,
                currentPeriodStart,
                currentPeriodEnd
        );
    }

    public static Subscription createForStripeCheckout(User user) {
        return new Subscription(user, SubscriptionStatus.INCOMPLETE, null, null);
    }

    public void activateDemo(
            OffsetDateTime currentPeriodStart,
            OffsetDateTime currentPeriodEnd
    ) {
        this.status = SubscriptionStatus.ACTIVE;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.cancelAtPeriodEnd = false;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void beginStripeCheckout(
            String stripeSubscriptionId,
            OffsetDateTime currentPeriodStart,
            OffsetDateTime currentPeriodEnd,
            boolean cancelAtPeriodEnd
    ) {
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.status = SubscriptionStatus.INCOMPLETE;
        updateStripePeriod(currentPeriodStart, currentPeriodEnd);
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void activateFromSuccessfulPayment(
            OffsetDateTime currentPeriodStart,
            OffsetDateTime currentPeriodEnd
    ) {
        this.status = this.cancelAtPeriodEnd
                ? SubscriptionStatus.CANCEL_AT_PERIOD_END
                : SubscriptionStatus.ACTIVE;
        updateStripePeriod(currentPeriodStart, currentPeriodEnd);
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void markPaymentFailed() {
        this.status = SubscriptionStatus.PAYMENT_FAILED;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void synchronizeFromStripe(
            SubscriptionStatus status,
            OffsetDateTime currentPeriodStart,
            OffsetDateTime currentPeriodEnd,
            boolean cancelAtPeriodEnd
    ) {
        this.status = status;
        if (currentPeriodStart != null && currentPeriodEnd != null) {
            updateStripePeriod(currentPeriodStart, currentPeriodEnd);
        }
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private void updateStripePeriod(
            OffsetDateTime currentPeriodStart,
            OffsetDateTime currentPeriodEnd
    ) {
        if (currentPeriodStart != null && currentPeriodEnd != null
                && !currentPeriodEnd.isAfter(currentPeriodStart)) {
            throw new IllegalArgumentException("Subscription period end must be after its start");
        }
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public OffsetDateTime getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public boolean isCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
