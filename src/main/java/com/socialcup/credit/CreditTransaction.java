package com.socialcup.credit;

import com.socialcup.membership.BillingCycle;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "credit_transactions")
public class CreditTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_cycle_id")
    private BillingCycle billingCycle;

    @Column(name = "redemption_id")
    private Long redemptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CreditTransactionType type;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected CreditTransaction() {
    }

    private CreditTransaction(
            User user,
            BillingCycle billingCycle,
            Long redemptionId,
            CreditTransactionType type,
            Integer amount
    ) {
        if (amount == 0) {
            throw new IllegalArgumentException("Credit transaction amount cannot be zero");
        }
        this.user = user;
        this.billingCycle = billingCycle;
        this.redemptionId = redemptionId;
        this.type = type;
        this.amount = amount;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    static CreditTransaction forCycle(
            User user,
            BillingCycle billingCycle,
            CreditTransactionType type,
            Integer amount
    ) {
        return new CreditTransaction(user, billingCycle, null, type, amount);
    }

    static CreditTransaction forRedemption(
            User user,
            Long redemptionId,
            Integer creditsSpent
    ) {
        if (redemptionId == null) {
            throw new IllegalArgumentException("Redemption ID is required");
        }
        if (creditsSpent == null || creditsSpent <= 0) {
            throw new IllegalArgumentException("Credits spent must be positive");
        }
        return new CreditTransaction(
                user,
                null,
                redemptionId,
                CreditTransactionType.REDEMPTION,
                -creditsSpent
        );
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public Long getRedemptionId() {
        return redemptionId;
    }

    public CreditTransactionType getType() {
        return type;
    }

    public Integer getAmount() {
        return amount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
