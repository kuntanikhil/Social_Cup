package com.socialcup.membership;

import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeCheckoutServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private StripeGateway stripeGateway;
    @Mock
    private User user;
    @Mock
    private Subscription localSubscription;

    private StripeCheckoutService service;

    @BeforeEach
    void setUp() {
        service = new StripeCheckoutService(
                userRepository,
                subscriptionRepository,
                stripeGateway
        );
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserId(7L))
                .thenReturn(Optional.of(localSubscription));
    }

    @Test
    void resumesIncompleteCheckoutWithoutCreatingAnotherSubscription() throws Exception {
        StripeCheckoutResponse expected = new StripeCheckoutResponse(
                "sub_existing",
                "pi_secret",
                "ek_fresh",
                "cus_existing"
        );
        when(localSubscription.getStatus()).thenReturn(SubscriptionStatus.INCOMPLETE);
        when(localSubscription.getStripeSubscriptionId()).thenReturn("sub_existing");
        when(localSubscription.getStripeCustomerId()).thenReturn("cus_existing");
        when(stripeGateway.resumeSubscriptionCheckout(
                "sub_existing",
                "cus_existing"
        )).thenReturn(expected);

        StripeCheckoutResponse result = service.createCheckout(7L);

        assertSame(expected, result);
        verify(stripeGateway).resumeSubscriptionCheckout(
                "sub_existing",
                "cus_existing"
        );
        verify(stripeGateway, never()).createIncompleteSubscription(any(), any());
        verify(subscriptionRepository, never()).save(localSubscription);
    }

    @Test
    void delegatesPaymentFailedCheckoutToStripeResumabilityCheck() throws Exception {
        StripeCheckoutResponse expected = new StripeCheckoutResponse(
                "sub_failed",
                "pi_retry_secret",
                "ek_fresh",
                "cus_existing"
        );
        when(localSubscription.getStatus()).thenReturn(SubscriptionStatus.PAYMENT_FAILED);
        when(localSubscription.getStripeSubscriptionId()).thenReturn("sub_failed");
        when(localSubscription.getStripeCustomerId()).thenReturn("cus_existing");
        when(stripeGateway.resumeSubscriptionCheckout(
                "sub_failed",
                "cus_existing"
        )).thenReturn(expected);

        assertSame(expected, service.createCheckout(7L));
        verify(stripeGateway, never()).createIncompleteSubscription(any(), any());
    }

    @Test
    void activeMembershipCannotStartAnotherCheckout() throws Exception {
        when(localSubscription.getStatus()).thenReturn(SubscriptionStatus.ACTIVE);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createCheckout(7L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(stripeGateway, never()).resumeSubscriptionCheckout(any(), any());
        verify(stripeGateway, never()).createIncompleteSubscription(any(), any());
    }
}
