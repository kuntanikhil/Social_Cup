package com.socialcup.drink;

import com.socialcup.cafe.Cafe;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "drinks")
public class Drink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafe_id", nullable = false)
    private Cafe cafe;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DrinkType type;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "photo_path", length = 500)
    private String photoPath;

    @Column(name = "retail_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "credit_price", nullable = false)
    private Integer creditPrice;

    @Column(nullable = false)
    private boolean signature;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Drink() {
    }

    private Drink(
            Cafe cafe,
            String name,
            DrinkType type,
            String description,
            String photoPath,
            BigDecimal retailPrice,
            Integer creditPrice,
            boolean signature
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.cafe = cafe;
        this.name = name;
        this.type = type;
        this.description = description;
        this.photoPath = photoPath;
        this.retailPrice = retailPrice;
        this.creditPrice = creditPrice;
        this.signature = signature;
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Drink create(
            Cafe cafe,
            String name,
            DrinkType type,
            String description,
            String photoPath,
            BigDecimal retailPrice,
            Integer creditPrice,
            boolean signature
    ) {
        return new Drink(
                cafe,
                name,
                type,
                description,
                photoPath,
                retailPrice,
                creditPrice,
                signature
        );
    }

    public void update(
            String name,
            DrinkType type,
            String description,
            String photoPath,
            BigDecimal retailPrice,
            Integer creditPrice,
            boolean signature,
            boolean active
    ) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.photoPath = photoPath;
        this.retailPrice = retailPrice;
        this.creditPrice = creditPrice;
        this.signature = signature;
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

    public Cafe getCafe() {
        return cafe;
    }

    public String getName() {
        return name;
    }

    public DrinkType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public BigDecimal getRetailPrice() {
        return retailPrice;
    }

    public Integer getCreditPrice() {
        return creditPrice;
    }

    public boolean isSignature() {
        return signature;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
