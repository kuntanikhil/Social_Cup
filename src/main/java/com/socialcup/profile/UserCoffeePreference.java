package com.socialcup.profile;

import com.socialcup.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_coffee_preferences")
@IdClass(UserCoffeePreferenceId.class)
public class UserCoffeePreference {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coffee_preference_id", nullable = false)
    private CoffeePreference coffeePreference;

    protected UserCoffeePreference() {
    }

    public UserCoffeePreference(User user, CoffeePreference coffeePreference) {
        this.user = user;
        this.coffeePreference = coffeePreference;
    }

    public User getUser() {
        return user;
    }

    public CoffeePreference getCoffeePreference() {
        return coffeePreference;
    }
}
