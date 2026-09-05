package com.socialcup.credit;

import com.socialcup.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "credit_accounts")
public class CreditAccount {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "credits_remaining", nullable = false)
    private Integer creditsRemaining;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CreditAccount() {
    }

    private CreditAccount(User user) {
        this.user = user;
        this.creditsRemaining = 0;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public static CreditAccount create(User user) {
        return new CreditAccount(user);
    }

    void resetTo(int credits) {
        if (credits < 0) {
            throw new IllegalArgumentException("Credit balance cannot be negative");
        }
        this.creditsRemaining = credits;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    void deduct(int credits) {
        if (credits <= 0) {
            throw new IllegalArgumentException("Credit deduction must be positive");
        }
        if (creditsRemaining < credits) {
            throw new InsufficientCreditsException();
        }
        this.creditsRemaining -= credits;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public Integer getCreditsRemaining() {
        return creditsRemaining;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
