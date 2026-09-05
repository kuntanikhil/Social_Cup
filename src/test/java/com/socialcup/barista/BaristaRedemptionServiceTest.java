package com.socialcup.barista;

import com.socialcup.cafe.Cafe;
import com.socialcup.credit.CreditService;
import com.socialcup.credit.InsufficientCreditsException;
import com.socialcup.drink.Drink;
import com.socialcup.membership.Subscription;
import com.socialcup.membership.SubscriptionRepository;
import com.socialcup.membership.SubscriptionStatus;
import com.socialcup.redemption.Redemption;
import com.socialcup.redemption.RedemptionRepository;
import com.socialcup.redemption.RedemptionSession;
import com.socialcup.redemption.RedemptionSessionRepository;
import com.socialcup.security.SecureTokenService;
import com.socialcup.settings.PlatformSetting;
import com.socialcup.settings.PlatformSettingRepository;
import com.socialcup.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BaristaRedemptionServiceTest {

    private static final String QR_TOKEN = "A".repeat(43);
    private static final AuthenticatedCafeDevice PRINCIPAL =
            new AuthenticatedCafeDevice(7L, 10L);

    @Mock
    private CafeDeviceService cafeDeviceService;
    @Mock
    private RedemptionSessionRepository sessionRepository;
    @Mock
    private RedemptionRepository redemptionRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private CreditService creditService;
    @Mock
    private PlatformSettingRepository platformSettingRepository;
    @Mock
    private SecureTokenService secureTokenService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CafeDevice device;
    @Mock
    private Cafe cafe;
    @Mock
    private Drink drink;
    @Mock
    private User member;
    @Mock
    private Subscription subscription;
    @Mock
    private PlatformSetting creditValueSetting;
    @Mock
    private Redemption savedRedemption;

    private BaristaRedemptionService service;

    @BeforeEach
    void setUp() {
        service = new BaristaRedemptionService(
                cafeDeviceService,
                sessionRepository,
                redemptionRepository,
                subscriptionRepository,
                creditService,
                platformSettingRepository,
                secureTokenService,
                passwordEncoder
        );
        when(cafeDeviceService.requireValidDeviceForUpdate(PRINCIPAL))
                .thenReturn(device);
        when(device.getCafe()).thenReturn(cafe);
        when(cafe.getId()).thenReturn(10L);
        when(cafe.getPayoutRatePerCredit()).thenReturn(new BigDecimal("0.65"));
        when(drink.getCafe()).thenReturn(cafe);
        when(drink.getId()).thenReturn(20L);
        when(drink.getName()).thenReturn("Vanilla Cloud Latte");
        when(member.getId()).thenReturn(30L);
        when(member.getDisplayName()).thenReturn("Nikhil Kumar");
        when(secureTokenService.hash(QR_TOKEN)).thenReturn("qr-hash");
        when(subscriptionRepository.findByUserIdForUpdate(30L))
                .thenReturn(Optional.of(subscription));
        when(subscription.getStatus()).thenReturn(SubscriptionStatus.ACTIVE);
        when(subscription.getCurrentPeriodStart())
                .thenReturn(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        when(subscription.getCurrentPeriodEnd())
                .thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(platformSettingRepository.findBySettingKey("CREDIT_VALUE"))
                .thenReturn(Optional.of(creditValueSetting));
        when(creditValueSetting.getSettingValue()).thenReturn("1.00");
        when(redemptionRepository.saveAndFlush(any(Redemption.class)))
                .thenReturn(savedRedemption);
        when(savedRedemption.getId()).thenReturn(50L);
        when(creditService.deductForRedemption(member, 50L, 5)).thenReturn(25);
    }

    @Test
    void validQrRedeemsExactlyOnce() {
        RedemptionSession session = liveSession(cafe);
        when(sessionRepository.findByQrTokenHashForUpdate("qr-hash"))
                .thenReturn(Optional.of(session));

        BaristaRedemptionResponse response = service.validate(
                PRINCIPAL,
                new ValidateRedemptionRequest(QR_TOKEN, null)
        );

        assertEquals("SUCCESS", response.result());
        assertEquals(5, response.creditsDeducted());
        assertEquals(25, response.creditsRemaining());
        assertEquals("Nikhil", response.member().firstName());
        verify(redemptionRepository).saveAndFlush(any(Redemption.class));
        verify(creditService).deductForRedemption(member, 50L, 5);
    }

    @Test
    void validBackupCodeRedeems() {
        RedemptionSession session = liveSession(cafe);
        when(sessionRepository.findAllForBackupCodeValidation())
                .thenReturn(List.of(session));
        when(passwordEncoder.matches("123456", "backup-hash"))
                .thenReturn(true);

        BaristaRedemptionResponse response = service.validate(
                PRINCIPAL,
                new ValidateRedemptionRequest(null, "123456")
        );

        assertEquals("SUCCESS", response.result());
        verify(creditService).deductForRedemption(member, 50L, 5);
    }

    @Test
    void wrongCafeIsRejected() {
        Cafe otherCafe = org.mockito.Mockito.mock(Cafe.class);
        when(otherCafe.getId()).thenReturn(99L);
        RedemptionSession session = liveSession(otherCafe);
        when(sessionRepository.findByQrTokenHashForUpdate("qr-hash"))
                .thenReturn(Optional.of(session));

        assertFailure(
                BaristaFailureReason.WRONG_CAFE,
                new ValidateRedemptionRequest(QR_TOKEN, null)
        );
        verify(creditService, never()).deductForRedemption(any(), any(), any(Integer.class));
    }

    @Test
    void expiredCodeIsRejected() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        RedemptionSession session = RedemptionSession.create(
                member,
                cafe,
                drink,
                5,
                "qr-hash",
                "backup-hash",
                now.minusMinutes(10),
                now.minusMinutes(5)
        );
        when(sessionRepository.findByQrTokenHashForUpdate("qr-hash"))
                .thenReturn(Optional.of(session));

        assertFailure(
                BaristaFailureReason.CODE_EXPIRED,
                new ValidateRedemptionRequest(QR_TOKEN, null)
        );
    }

    @Test
    void alreadyUsedCodeIsRejected() {
        RedemptionSession session = liveSession(cafe);
        session.markRedeemed(OffsetDateTime.now(ZoneOffset.UTC));
        when(sessionRepository.findByQrTokenHashForUpdate("qr-hash"))
                .thenReturn(Optional.of(session));

        assertFailure(
                BaristaFailureReason.CODE_ALREADY_USED,
                new ValidateRedemptionRequest(QR_TOKEN, null)
        );
    }

    @Test
    void inactiveMembershipIsRejected() {
        RedemptionSession session = liveSession(cafe);
        when(sessionRepository.findByQrTokenHashForUpdate("qr-hash"))
                .thenReturn(Optional.of(session));
        when(subscription.getStatus()).thenReturn(SubscriptionStatus.PAYMENT_FAILED);

        assertFailure(
                BaristaFailureReason.MEMBERSHIP_INACTIVE,
                new ValidateRedemptionRequest(QR_TOKEN, null)
        );
    }

    @Test
    void insufficientCreditsRollsBackAsFailure() {
        RedemptionSession session = liveSession(cafe);
        when(sessionRepository.findByQrTokenHashForUpdate("qr-hash"))
                .thenReturn(Optional.of(session));
        when(creditService.deductForRedemption(member, 50L, 5))
                .thenThrow(new InsufficientCreditsException());

        assertFailure(
                BaristaFailureReason.INSUFFICIENT_CREDITS,
                new ValidateRedemptionRequest(QR_TOKEN, null)
        );
    }

    @Test
    void replayCreatesOnlyOneRedemptionAndDeduction() {
        RedemptionSession session = liveSession(cafe);
        when(sessionRepository.findByQrTokenHashForUpdate("qr-hash"))
                .thenReturn(Optional.of(session));

        service.validate(PRINCIPAL, new ValidateRedemptionRequest(QR_TOKEN, null));
        assertFailure(
                BaristaFailureReason.CODE_ALREADY_USED,
                new ValidateRedemptionRequest(QR_TOKEN, null)
        );

        verify(redemptionRepository, times(1)).saveAndFlush(any(Redemption.class));
        verify(creditService, times(1)).deductForRedemption(member, 50L, 5);
    }

    @Test
    void bothOrNeitherCodeIsRejected() {
        assertFailure(
                BaristaFailureReason.INVALID_CODE,
                new ValidateRedemptionRequest(QR_TOKEN, "123456")
        );
        assertFailure(
                BaristaFailureReason.INVALID_CODE,
                new ValidateRedemptionRequest(null, null)
        );
        verify(sessionRepository, never()).findByQrTokenHashForUpdate(any());
        verify(sessionRepository, never()).findAllForBackupCodeValidation();
    }

    private RedemptionSession liveSession(Cafe sessionCafe) {
        when(drink.getCafe()).thenReturn(sessionCafe);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return RedemptionSession.create(
                member,
                sessionCafe,
                drink,
                5,
                "qr-hash",
                "backup-hash",
                now.minusSeconds(5),
                now.plusMinutes(5)
        );
    }

    private void assertFailure(
            BaristaFailureReason reason,
            ValidateRedemptionRequest request
    ) {
        BaristaApiException exception = assertThrows(
                BaristaApiException.class,
                () -> service.validate(PRINCIPAL, request)
        );
        assertEquals(reason, exception.getReason());
    }
}
