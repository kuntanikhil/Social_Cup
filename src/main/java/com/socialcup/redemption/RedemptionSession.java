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
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "redemption_sessions")
public class RedemptionSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafe_id", nullable = false)
    private Cafe cafe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drink_id", nullable = false)
    private Drink drink;

    @Column(name = "credit_cost", nullable = false)
    private Integer creditCost;

    @Column(name = "qr_token_hash", nullable = false, unique = true, length = 255)
    private String qrTokenHash;

    @Column(name = "backup_code_hash", nullable = false, unique = true, length = 255)
    private String backupCodeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RedemptionSessionStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    protected RedemptionSession() {
    }

    private RedemptionSession(
            User member,
            Cafe cafe,
            Drink drink,
            Integer creditCost,
            String qrTokenHash,
            String backupCodeHash,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt
    ) {
        if (creditCost == null || creditCost <= 0) {
            throw new IllegalArgumentException("Redemption credit cost must be positive");
        }
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Redemption expiry must follow creation");
        }
        this.member = member;
        this.cafe = cafe;
        this.drink = drink;
        this.creditCost = creditCost;
        this.qrTokenHash = qrTokenHash;
        this.backupCodeHash = backupCodeHash;
        this.status = RedemptionSessionStatus.ACTIVE;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static RedemptionSession create(
            User member,
            Cafe cafe,
            Drink drink,
            Integer creditCost,
            String qrTokenHash,
            String backupCodeHash,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt
    ) {
        return new RedemptionSession(
                member,
                cafe,
                drink,
                creditCost,
                qrTokenHash,
                backupCodeHash,
                createdAt,
                expiresAt
        );
    }

    public boolean isExpiredAt(OffsetDateTime serverTime) {
        return !expiresAt.isAfter(serverTime);
    }

    public void cancel(OffsetDateTime cancelledAt) {
        if (status == RedemptionSessionStatus.ACTIVE) {
            this.status = RedemptionSessionStatus.CANCELLED;
            this.cancelledAt = cancelledAt;
        }
    }

    public void expire() {
        if (status == RedemptionSessionStatus.ACTIVE) {
            this.status = RedemptionSessionStatus.EXPIRED;
        }
    }

    public void markRedeemed(OffsetDateTime consumedAt) {
        if (status != RedemptionSessionStatus.ACTIVE) {
            throw new IllegalStateException("Only an active session can be redeemed");
        }
        this.status = RedemptionSessionStatus.REDEEMED;
        this.consumedAt = consumedAt;
    }

    public Long getId() {
        return id;
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

    public Integer getCreditCost() {
        return creditCost;
    }

    public String getQrTokenHash() {
        return qrTokenHash;
    }

    public String getBackupCodeHash() {
        return backupCodeHash;
    }

    public RedemptionSessionStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public OffsetDateTime getConsumedAt() {
        return consumedAt;
    }
}
