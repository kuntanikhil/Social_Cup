package com.socialcup.membership;

import com.socialcup.credit.CreditService;
import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class MembershipService {

    private final SubscriptionRepository subscriptionRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final CreditService creditService;
    private final UserRepository userRepository;

    public MembershipService(
            SubscriptionRepository subscriptionRepository,
            BillingCycleRepository billingCycleRepository,
            CreditService creditService,
            UserRepository userRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.billingCycleRepository = billingCycleRepository;
        this.creditService = creditService;
        this.userRepository = userRepository;
    }

    @Transactional
    public MembershipResponse getMembership(Long userId) {
        User user = getUserForUpdate(userId);
        int creditsRemaining = creditService.getBalance(user);
        return subscriptionRepository.findByUserId(userId)
                .map(subscription -> toResponse(subscription, creditsRemaining))
                .orElseGet(() -> new MembershipResponse(
                        SubscriptionStatus.NONE,
                        false,
                        creditsRemaining,
                        null,
                        false
                ));
    }

    @Transactional
    public MembershipResponse demoActivate(Long userId) {
        User user = getUserForUpdate(userId);
        OffsetDateTime periodStart = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime periodEnd = periodStart.plusMonths(1);

        Optional<Subscription> existingSubscription = subscriptionRepository.findByUserId(userId);
        Subscription subscription;
        if (existingSubscription.isPresent()) {
            subscription = existingSubscription.get();
            subscription.activateDemo(periodStart, periodEnd);
        } else {
            subscription = Subscription.createDemo(user, periodStart, periodEnd);
        }
        subscription = subscriptionRepository.save(subscription);

        BillingCycle billingCycle = billingCycleRepository.save(
                BillingCycle.createProcessedDemo(
                        subscription,
                        periodStart,
                        periodEnd,
                        periodStart
                )
        );
        int creditsRemaining = creditService.resetForSuccessfulCycle(
                user,
                billingCycle
        );

        return toResponse(subscription, creditsRemaining);
    }

    private User getUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    private MembershipResponse toResponse(
            Subscription subscription,
            int creditsRemaining
    ) {
        return new MembershipResponse(
                subscription.getStatus(),
                subscription.getStatus().isMember(),
                creditsRemaining,
                subscription.getCurrentPeriodEnd(),
                subscription.isCancelAtPeriodEnd()
        );
    }
}
