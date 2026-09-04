package com.socialcup.profile;

import java.io.Serializable;
import java.util.Objects;

public class UserCoffeePreferenceId implements Serializable {

    private Long user;
    private Long coffeePreference;

    public UserCoffeePreferenceId() {
    }

    public UserCoffeePreferenceId(Long user, Long coffeePreference) {
        this.user = user;
        this.coffeePreference = coffeePreference;
    }

    public Long getUser() {
        return user;
    }

    public Long getCoffeePreference() {
        return coffeePreference;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof UserCoffeePreferenceId that)) {
            return false;
        }
        return Objects.equals(user, that.user)
                && Objects.equals(coffeePreference, that.coffeePreference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, coffeePreference);
    }
}
