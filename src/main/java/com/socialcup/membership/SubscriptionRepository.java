package com.socialcup.membership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserId(Long userId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<Subscription> findFirstByStripeCustomerId(String stripeCustomerId);

    @Query("""
            select s.user.id
            from Subscription s
            where s.stripeSubscriptionId = :stripeSubscriptionId
            """)
    Optional<Long> findUserIdByStripeSubscriptionId(
            @Param("stripeSubscriptionId") String stripeSubscriptionId
    );

    @Query("""
            select s.user.id
            from Subscription s
            where s.stripeCustomerId = :stripeCustomerId
            """)
    Optional<Long> findUserIdByStripeCustomerId(
            @Param("stripeCustomerId") String stripeCustomerId
    );
}
