package com.socialcup.cafe;

import com.socialcup.neighbourhood.Neighbourhood;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "cafes")
public class Cafe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "neighbourhood_id", nullable = false)
    private Neighbourhood neighbourhood;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "perk_line", length = 500)
    private String perkLine;

    @Column(name = "payout_rate_per_credit", nullable = false, precision = 10, scale = 2)
    private BigDecimal payoutRatePerCredit;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "scan_slug", nullable = false, unique = true, length = 255)
    private String scanSlug;

    @Column(name = "pin_hash", length = 255)
    private String pinHash;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Cafe() {
    }

    private Cafe(
            String name,
            String address,
            Neighbourhood neighbourhood,
            BigDecimal latitude,
            BigDecimal longitude,
            String perkLine,
            BigDecimal payoutRatePerCredit,
            boolean featured,
            String scanSlug
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.name = name;
        this.address = address;
        this.neighbourhood = neighbourhood;
        this.latitude = latitude;
        this.longitude = longitude;
        this.perkLine = perkLine;
        this.payoutRatePerCredit = payoutRatePerCredit;
        this.featured = featured;
        this.active = true;
        this.scanSlug = scanSlug;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Cafe create(
            String name,
            String address,
            Neighbourhood neighbourhood,
            BigDecimal latitude,
            BigDecimal longitude,
            String perkLine,
            BigDecimal payoutRatePerCredit,
            boolean featured,
            String scanSlug
    ) {
        return new Cafe(
                name,
                address,
                neighbourhood,
                latitude,
                longitude,
                perkLine,
                payoutRatePerCredit,
                featured,
                scanSlug
        );
    }

    public void update(
            String name,
            String address,
            Neighbourhood neighbourhood,
            BigDecimal latitude,
            BigDecimal longitude,
            String perkLine,
            BigDecimal payoutRatePerCredit,
            boolean featured,
            boolean active
    ) {
        this.name = name;
        this.address = address;
        this.neighbourhood = neighbourhood;
        this.latitude = latitude;
        this.longitude = longitude;
        this.perkLine = perkLine;
        this.payoutRatePerCredit = payoutRatePerCredit;
        this.featured = featured;
        this.active = active;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public Neighbourhood getNeighbourhood() {
        return neighbourhood;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getPerkLine() {
        return perkLine;
    }

    public BigDecimal getPayoutRatePerCredit() {
        return payoutRatePerCredit;
    }

    public boolean isFeatured() {
        return featured;
    }

    public boolean isActive() {
        return active;
    }

    public String getScanSlug() {
        return scanSlug;
    }

    public String getPinHash() {
        return pinHash;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
