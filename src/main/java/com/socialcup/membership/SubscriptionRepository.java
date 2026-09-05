package com.socialcup.membership;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select subscription from Subscription subscription where subscription.user.id = :userId")
    Optional<Subscription> findByUserIdForUpdate(@Param("userId") Long userId);

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
