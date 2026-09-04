package com.socialcup.rating;

import com.socialcup.drink.Drink;
import com.socialcup.user.User;
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
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "drink_ratings",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_drink_ratings_user_drink",
                columnNames = {"user_id", "drink_id"}
        )
)
public class DrinkRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drink_id", nullable = false)
    private Drink drink;

    @Column(nullable = false)
    private Integer stars;

    @Column(length = 140)
    private String note;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected DrinkRating() {
    }

    private DrinkRating(User user, Drink drink, Integer stars, String note) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.user = user;
        this.drink = drink;
        this.stars = stars;
        this.note = note;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static DrinkRating create(User user, Drink drink, Integer stars, String note) {
        return new DrinkRating(user, drink, stars, note);
    }

    public void update(Integer stars, String note) {
        this.stars = stars;
        this.note = note;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
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

    public Drink getDrink() {
        return drink;
    }

    public Integer getStars() {
        return stars;
    }

    public String getNote() {
        return note;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
