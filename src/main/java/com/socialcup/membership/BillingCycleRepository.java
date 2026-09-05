package com.socialcup.membership;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingCycleRepository extends JpaRepository<BillingCycle, Long> {

    Optional<BillingCycle> findByStripeReference(String stripeReference);
}
