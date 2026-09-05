package com.socialcup.redemption;

import com.socialcup.cafe.Cafe;
import com.socialcup.cafe.CafeRepository;
import com.socialcup.credit.CreditService;
import com.socialcup.drink.Drink;
import com.socialcup.drink.DrinkRepository;
import com.socialcup.membership.Subscription;
import com.socialcup.membership.SubscriptionRepository;
import com.socialcup.membership.SubscriptionStatus;
import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedemptionSessionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long CAFE_ID = 10L;
    private static final Long DRINK_ID = 20L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private CafeRepository cafeRepository;
    @Mock
    private DrinkRepository drinkRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private CreditService creditService;
    @Mock
    private RedemptionSessionRepository redemptionSessionRepository;
    @Mock
    private RedemptionCredentialGenerator credentialGenerator;
    @Mock
    private User member;
    @Mock
    private Cafe cafe;
    @Mock
    private Drink drink;
    @Mock
    private Subscription subscription;

    private RedemptionSessionService service;

    @BeforeEach
    void setUp() {
        service = new RedemptionSessionService(
                userRepository,
                cafeRepository,
                drinkRepository,
                subscriptionRepository,
                creditService,
                redemptionSessionRepository,
                credentialGenerator
        );
    }

    @Test
    void activeMemberCreatesSessionWithoutChangingCredits() {
        arrangeEligibleMember(30);
        arrangeSuccessfulCreation(List.of());

        CreateRedemptionSessionResponse response = service.create(
                USER_ID,
                CAFE_ID,
                new CreateRedemptionSessionRequest(DRINK_ID)
        );

        assertEquals(30, response.creditsBefore());
        assertEquals(25, response.creditsAfter());
        assertEquals("PENDING", response.status());
        assertEquals("raw-qr", response.qrToken());
        assertEquals("123456", response.backupCode());
        assertNotNull(response.expiresAt());
        verify(creditService).getBalance(member);
        verify(creditService, never()).resetForSuccessfulCycle(any(), any());
    }

    @Test
    void visitorWithoutActiveMembershipIsRejected() {
        arrangeCafeAndDrink();
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(member));
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        USER_ID,
                        CAFE_ID,
                        new CreateRedemptionSessionRequest(DRINK_ID)
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(creditService, never()).getBalance(any());
    }

    @Test
    void insufficientCreditsAreRejected() {
        arrangeEligibleMember(4);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        USER_ID,
                        CAFE_ID,
                        new CreateRedemptionSessionRequest(DRINK_ID)
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(redemptionSessionRepository, never()).save(any());
    }

    @Test
    void drinkFromAnotherCafeIsRejected() {
        Cafe otherCafe = org.mockito.Mockito.mock(Cafe.class);
        when(otherCafe.getId()).thenReturn(99L);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(member));
        when(cafeRepository.findByIdAndActiveTrue(CAFE_ID)).thenReturn(Optional.of(cafe));
        when(cafe.getId()).thenReturn(CAFE_ID);
        when(drinkRepository.findByIdAndActiveTrue(DRINK_ID)).thenReturn(Optional.of(drink));
        when(drink.getCafe()).thenReturn(otherCafe);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        USER_ID,
                        CAFE_ID,
                        new CreateRedemptionSessionRequest(DRINK_ID)
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(subscriptionRepository, never()).findByUserId(any());
    }

    @Test
    void secondSessionCancelsFirstLiveSession() {
        arrangeEligibleMember(30);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        RedemptionSession existing = RedemptionSession.create(
                member,
                cafe,
                drink,
                5,
                "old-qr-hash",
                "old-backup-hash",
                now.minusMinutes(1),
                now.plusMinutes(4)
        );
        arrangeSuccessfulCreation(List.of(existing));

        service.create(USER_ID, CAFE_ID, new CreateRedemptionSessionRequest(DRINK_ID));

        assertEquals(RedemptionSessionStatus.CANCELLED, existing.getStatus());
        assertNotNull(existing.getCancelledAt());
    }

    @Test
    void expiredSessionIsReportedAndPersistedAsExpired() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        RedemptionSession expired = RedemptionSession.create(
                member,
                cafe,
                drink,
                5,
                "qr-hash",
                "backup-hash",
                now.minusMinutes(10),
                now.minusMinutes(5)
        );
        when(member.getId()).thenReturn(USER_ID);
        when(cafe.getId()).thenReturn(CAFE_ID);
        when(cafe.getName()).thenReturn("Social Brew");
        when(drink.getId()).thenReturn(DRINK_ID);
        when(drink.getName()).thenReturn("Vanilla Latte");
        when(redemptionSessionRepository.findDetailsById(100L))
                .thenReturn(Optional.of(expired));

        RedemptionSessionResponse response = service.get(USER_ID, 100L);

        assertEquals("EXPIRED", response.status());
        assertEquals(RedemptionSessionStatus.EXPIRED, expired.getStatus());
    }

    private void arrangeEligibleMember(int balance) {
        arrangeCafeAndDrink();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(member));
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(subscription));
        when(subscription.getStatus()).thenReturn(SubscriptionStatus.ACTIVE);
        when(subscription.getCurrentPeriodStart()).thenReturn(now.minusDays(1));
        when(subscription.getCurrentPeriodEnd()).thenReturn(now.plusDays(1));
        when(drink.getCreditPrice()).thenReturn(5);
        when(creditService.getBalance(member)).thenReturn(balance);
    }

    private void arrangeSuccessfulCreation(List<RedemptionSession> existingSessions) {
        when(member.getId()).thenReturn(USER_ID);
        when(redemptionSessionRepository.findByMemberIdAndStatus(
                USER_ID,
                RedemptionSessionStatus.ACTIVE
        )).thenReturn(existingSessions);
        when(credentialGenerator.generate()).thenReturn(
                new RedemptionCredentialGenerator.GeneratedRedemptionCredentials(
                        "raw-qr",
                        "qr-hash",
                        "123456",
                        "backup-hash"
                )
        );
        when(redemptionSessionRepository.save(any(RedemptionSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void arrangeCafeAndDrink() {
        when(cafeRepository.findByIdAndActiveTrue(CAFE_ID)).thenReturn(Optional.of(cafe));
        when(cafe.getId()).thenReturn(CAFE_ID);
        when(drinkRepository.findByIdAndActiveTrue(DRINK_ID)).thenReturn(Optional.of(drink));
        when(drink.getCafe()).thenReturn(cafe);
    }
}
