package com.socialcup.membership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StripeEventRepository extends JpaRepository<StripeEvent, Long> {

    Optional<StripeEvent> findByStripeEventId(String stripeEventId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO stripe_events (stripe_event_id, event_type)
            VALUES (:eventId, :eventType)
            ON CONFLICT (stripe_event_id) DO NOTHING
            """, nativeQuery = true)
    int claim(
            @Param("eventId") String eventId,
            @Param("eventType") String eventType
    );
}
