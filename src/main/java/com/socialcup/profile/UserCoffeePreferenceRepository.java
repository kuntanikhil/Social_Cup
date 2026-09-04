package com.socialcup.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserCoffeePreferenceRepository
        extends JpaRepository<UserCoffeePreference, UserCoffeePreferenceId> {

    @Query("""
            select ucp.coffeePreference
            from UserCoffeePreference ucp
            where ucp.user.id = :userId
            order by ucp.coffeePreference.id
            """)
    List<CoffeePreference> findCoffeePreferencesByUserId(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true)
    @Query("delete from UserCoffeePreference ucp where ucp.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
