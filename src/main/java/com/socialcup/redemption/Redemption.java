package com.socialcup.redemption;

import com.socialcup.cafe.Cafe;
import com.socialcup.drink.Drink;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "redemptions")
public class Redemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private RedemptionSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafe_id", nullable = false)
    private Cafe cafe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drink_id", nullable = false)
    private Drink drink;

    @Column(name = "credits_spent", nullable = false)
    private Integer creditsSpent;

    @Column(name = "credit_value_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal creditValueSnapshot;

    @Column(name = "member_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal memberValue;

    @Column(name = "payout_rate_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal payoutRateSnapshot;

    @Column(name = "payout_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal payoutAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal margin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RedemptionStatus status;

    @Column(name = "redeemed_at", nullable = false)
    private OffsetDateTime redeemedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Redemption() {
    }

    private Redemption(
            RedemptionSession session,
            BigDecimal creditValueSnapshot,
            BigDecimal memberValue,
            BigDecimal payoutRateSnapshot,
            BigDecimal payoutAmount,
            BigDecimal margin,
            OffsetDateTime redeemedAt
    ) {
        this.session = session;
        this.member = session.getMember();
        this.cafe = session.getCafe();
        this.drink = session.getDrink();
        this.creditsSpent = session.getCreditCost();
        this.creditValueSnapshot = creditValueSnapshot;
        this.memberValue = memberValue;
        this.payoutRateSnapshot = payoutRateSnapshot;
        this.payoutAmount = payoutAmount;
        this.margin = margin;
        this.status = RedemptionStatus.COMPLETED;
        this.redeemedAt = redeemedAt;
        this.createdAt = redeemedAt;
    }

    public static Redemption complete(
            RedemptionSession session,
            BigDecimal creditValueSnapshot,
            BigDecimal memberValue,
            BigDecimal payoutRateSnapshot,
            BigDecimal payoutAmount,
            BigDecimal margin,
            OffsetDateTime redeemedAt
    ) {
        return new Redemption(
                session,
                creditValueSnapshot,
                memberValue,
                payoutRateSnapshot,
                payoutAmount,
                margin,
                redeemedAt
        );
    }

    public Long getId() {
        return id;
    }

    public RedemptionSession getSession() {
        return session;
    }

    public User getMember() {
        return member;
    }

    public Cafe getCafe() {
        return cafe;
    }

    public Drink getDrink() {
        return drink;
    }

    public Integer getCreditsSpent() {
        return creditsSpent;
    }

    public BigDecimal getCreditValueSnapshot() {
        return creditValueSnapshot;
    }

    public BigDecimal getMemberValue() {
        return memberValue;
    }

    public BigDecimal getPayoutRateSnapshot() {
        return payoutRateSnapshot;
    }

    public BigDecimal getPayoutAmount() {
        return payoutAmount;
    }

    public BigDecimal getMargin() {
        return margin;
    }

    public RedemptionStatus getStatus() {
        return status;
    }

    public OffsetDateTime getRedeemedAt() {
        return redeemedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
