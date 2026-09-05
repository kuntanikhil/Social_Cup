package com.socialcup.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "billing_cycles")
public class BillingCycle {

    public static final String STATUS_PROCESSED = "PROCESSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "period_start", nullable = false)
    private OffsetDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private OffsetDateTime periodEnd;

    @Column(name = "stripe_reference", unique = true, length = 255)
    private String stripeReference;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected BillingCycle() {
    }

    private BillingCycle(
            Subscription subscription,
            OffsetDateTime periodStart,
            OffsetDateTime periodEnd,
            String stripeReference,
            String status,
            OffsetDateTime processedAt
    ) {
        this.subscription = subscription;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.stripeReference = stripeReference;
        this.status = status;
        this.processedAt = processedAt;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public static BillingCycle createProcessedDemo(
            Subscription subscription,
            OffsetDateTime periodStart,
            OffsetDateTime periodEnd,
            OffsetDateTime processedAt
    ) {
        return new BillingCycle(
                subscription,
                periodStart,
                periodEnd,
                null,
                STATUS_PROCESSED,
                processedAt
        );
    }

    public static BillingCycle createProcessedStripe(
            Subscription subscription,
            OffsetDateTime periodStart,
            OffsetDateTime periodEnd,
            String stripeReference,
            OffsetDateTime processedAt
    ) {
        return new BillingCycle(
                subscription,
                periodStart,
                periodEnd,
                stripeReference,
                STATUS_PROCESSED,
                processedAt
        );
    }

    public Long getId() {
        return id;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public OffsetDateTime getPeriodStart() {
        return periodStart;
    }

    public OffsetDateTime getPeriodEnd() {
        return periodEnd;
    }

    public String getStripeReference() {
        return stripeReference;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
