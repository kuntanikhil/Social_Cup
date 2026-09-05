package com.socialcup.barista;

import com.socialcup.credit.CreditService;
import com.socialcup.credit.InsufficientCreditsException;
import com.socialcup.membership.Subscription;
import com.socialcup.membership.SubscriptionRepository;
import com.socialcup.redemption.Redemption;
import com.socialcup.redemption.RedemptionRepository;
import com.socialcup.redemption.RedemptionSession;
import com.socialcup.redemption.RedemptionSessionRepository;
import com.socialcup.redemption.RedemptionSessionStatus;
import com.socialcup.redemption.RedemptionStatus;
import com.socialcup.security.SecureTokenService;
import com.socialcup.settings.PlatformSetting;
import com.socialcup.settings.PlatformSettingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Service
public class BaristaRedemptionService {

    private static final String CREDIT_VALUE_SETTING = "CREDIT_VALUE";
    private static final ZoneId DALLAS_TIME_ZONE = ZoneId.of("America/Chicago");
    private static final int QR_TOKEN_LENGTH = 43;

    private final CafeDeviceService cafeDeviceService;
    private final RedemptionSessionRepository redemptionSessionRepository;
    private final RedemptionRepository redemptionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CreditService creditService;
    private final PlatformSettingRepository platformSettingRepository;
    private final SecureTokenService secureTokenService;
    private final PasswordEncoder passwordEncoder;

    public BaristaRedemptionService(
            CafeDeviceService cafeDeviceService,
            RedemptionSessionRepository redemptionSessionRepository,
            RedemptionRepository redemptionRepository,
            SubscriptionRepository subscriptionRepository,
            CreditService creditService,
            PlatformSettingRepository platformSettingRepository,
            SecureTokenService secureTokenService,
            PasswordEncoder passwordEncoder
    ) {
        this.cafeDeviceService = cafeDeviceService;
        this.redemptionSessionRepository = redemptionSessionRepository;
        this.redemptionRepository = redemptionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.creditService = creditService;
        this.platformSettingRepository = platformSettingRepository;
        this.secureTokenService = secureTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public BaristaRedemptionResponse validate(
            AuthenticatedCafeDevice principal,
            ValidateRedemptionRequest request
    ) {
        CafeDevice device = cafeDeviceService.requireValidDeviceForUpdate(principal);
        RedemptionSession session = findSessionForUpdate(
                request,
                device.getCafe().getId()
        );
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        validateSessionState(session, now);
        if (!Objects.equals(session.getCafe().getId(), device.getCafe().getId())) {
            throw failure(HttpStatus.FORBIDDEN, BaristaFailureReason.WRONG_CAFE);
        }
        if (!Objects.equals(
                session.getDrink().getCafe().getId(),
                session.getCafe().getId()
        )) {
            throw failure(HttpStatus.BAD_REQUEST, BaristaFailureReason.INVALID_CODE);
        }

        Subscription subscription = subscriptionRepository
                .findByUserIdForUpdate(session.getMember().getId())
                .orElseThrow(() -> failure(
                        HttpStatus.FORBIDDEN,
                        BaristaFailureReason.MEMBERSHIP_INACTIVE
                ));
        validateMembership(subscription, now);

        FinancialSnapshot financial = financialSnapshot(session);
        session.markRedeemed(now);
        Redemption redemption = redemptionRepository.saveAndFlush(
                Redemption.complete(
                        session,
                        financial.creditValue(),
                        financial.memberValue(),
                        financial.payoutRate(),
                        financial.payoutAmount(),
                        financial.margin(),
                        now
                )
        );

        int creditsRemaining;
        try {
            creditsRemaining = creditService.deductForRedemption(
                    session.getMember(),
                    redemption.getId(),
                    session.getCreditCost()
            );
        } catch (InsufficientCreditsException exception) {
            throw failure(
                    HttpStatus.CONFLICT,
                    BaristaFailureReason.INSUFFICIENT_CREDITS
            );
        }

        return new BaristaRedemptionResponse(
                "SUCCESS",
                redemption.getId(),
                new BaristaMemberResponse(
                        firstName(session.getMember().getDisplayName()),
                        session.getMember().getProfilePhotoPath()
                ),
                new BaristaDrinkResponse(
                        session.getDrink().getId(),
                        session.getDrink().getName()
                ),
                session.getCreditCost(),
                creditsRemaining
        );
    }

    @Transactional(readOnly = true)
    public List<BaristaTodayRedemptionResponse> today(
            AuthenticatedCafeDevice principal
    ) {
        CafeDevice device = cafeDeviceService.requireValidDevice(principal);
        LocalDate cafeDate = LocalDate.now(DALLAS_TIME_ZONE);
        OffsetDateTime start = cafeDate.atStartOfDay(DALLAS_TIME_ZONE)
                .toOffsetDateTime()
                .withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime end = cafeDate.plusDays(1)
                .atStartOfDay(DALLAS_TIME_ZONE)
                .toOffsetDateTime()
                .withOffsetSameInstant(ZoneOffset.UTC);

        return redemptionRepository
                .findByCafeIdAndStatusAndRedeemedAtGreaterThanEqualAndRedeemedAtLessThanOrderByRedeemedAtDesc(
                        device.getCafe().getId(),
                        RedemptionStatus.COMPLETED,
                        start,
                        end
                )
                .stream()
                .map(redemption -> new BaristaTodayRedemptionResponse(
                        redemption.getId(),
                        firstName(redemption.getMember().getDisplayName()),
                        redemption.getDrink().getName(),
                        redemption.getCreditsSpent(),
                        redemption.getRedeemedAt()
                ))
                .toList();
    }

    private RedemptionSession findSessionForUpdate(
            ValidateRedemptionRequest request,
            Long authenticatedCafeId
    ) {
        boolean hasQrToken = hasValue(request.qrToken());
        boolean hasBackupCode = hasValue(request.backupCode());
        if (hasQrToken == hasBackupCode) {
            throw failure(HttpStatus.BAD_REQUEST, BaristaFailureReason.INVALID_CODE);
        }

        if (hasQrToken) {
            if (request.qrToken().length() != QR_TOKEN_LENGTH) {
                throw failure(HttpStatus.BAD_REQUEST, BaristaFailureReason.INVALID_CODE);
            }
            return redemptionSessionRepository.findByQrTokenHashForUpdate(
                            secureTokenService.hash(request.qrToken())
                    )
                    .orElseThrow(() -> failure(
                            HttpStatus.BAD_REQUEST,
                            BaristaFailureReason.INVALID_CODE
                    ));
        }

        if (!request.backupCode().matches("\\d{6}")) {
            throw failure(HttpStatus.BAD_REQUEST, BaristaFailureReason.INVALID_CODE);
        }

        List<RedemptionSession> matches = redemptionSessionRepository
                .findAllForBackupCodeValidation()
                .stream()
                .filter(session -> passwordEncoder.matches(
                        request.backupCode(),
                        session.getBackupCodeHash()
                ))
                .toList();
        if (matches.isEmpty()) {
            throw failure(HttpStatus.BAD_REQUEST, BaristaFailureReason.INVALID_CODE);
        }

        List<RedemptionSession> cafeMatches = matches.stream()
                .filter(session -> Objects.equals(
                        session.getCafe().getId(),
                        authenticatedCafeId
                ))
                .toList();
        if (cafeMatches.size() == 1) {
            return cafeMatches.getFirst();
        }
        if (cafeMatches.size() > 1 || matches.size() > 1) {
            throw failure(HttpStatus.BAD_REQUEST, BaristaFailureReason.INVALID_CODE);
        }
        return matches.getFirst();
    }

    private void validateSessionState(
            RedemptionSession session,
            OffsetDateTime now
    ) {
        if (session.getStatus() == RedemptionSessionStatus.REDEEMED) {
            throw failure(
                    HttpStatus.CONFLICT,
                    BaristaFailureReason.CODE_ALREADY_USED
            );
        }
        if (session.getStatus() == RedemptionSessionStatus.EXPIRED
                || session.isExpiredAt(now)) {
            throw failure(HttpStatus.GONE, BaristaFailureReason.CODE_EXPIRED);
        }
        if (session.getStatus() != RedemptionSessionStatus.ACTIVE) {
            throw failure(HttpStatus.BAD_REQUEST, BaristaFailureReason.INVALID_CODE);
        }
    }

    private void validateMembership(
            Subscription subscription,
            OffsetDateTime now
    ) {
        boolean hasStarted = subscription.getCurrentPeriodStart() == null
                || !subscription.getCurrentPeriodStart().isAfter(now);
        boolean hasNotEnded = subscription.getCurrentPeriodEnd() != null
                && subscription.getCurrentPeriodEnd().isAfter(now);
        if (!subscription.getStatus().isMember() || !hasStarted || !hasNotEnded) {
            throw failure(
                    HttpStatus.FORBIDDEN,
                    BaristaFailureReason.MEMBERSHIP_INACTIVE
            );
        }
    }

    private FinancialSnapshot financialSnapshot(RedemptionSession session) {
        PlatformSetting setting = platformSettingRepository
                .findBySettingKey(CREDIT_VALUE_SETTING)
                .orElseThrow(() -> new IllegalStateException(
                        "CREDIT_VALUE platform setting is missing"
                ));
        BigDecimal creditValue;
        try {
            creditValue = new BigDecimal(setting.getSettingValue())
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "CREDIT_VALUE platform setting is invalid",
                    exception
            );
        }
        if (creditValue.signum() < 0) {
            throw new IllegalStateException("CREDIT_VALUE cannot be negative");
        }

        BigDecimal credits = BigDecimal.valueOf(session.getCreditCost());
        BigDecimal payoutRate = session.getCafe().getPayoutRatePerCredit()
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal memberValue = credits.multiply(creditValue)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal payoutAmount = credits.multiply(payoutRate)
                .setScale(2, RoundingMode.HALF_UP);
        return new FinancialSnapshot(
                creditValue,
                memberValue,
                payoutRate,
                payoutAmount,
                memberValue.subtract(payoutAmount)
                        .setScale(2, RoundingMode.HALF_UP)
        );
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    private String firstName(String displayName) {
        String normalized = displayName == null ? "" : displayName.trim();
        int separator = normalized.indexOf(' ');
        return separator < 0 ? normalized : normalized.substring(0, separator);
    }

    private BaristaApiException failure(
            HttpStatus status,
            BaristaFailureReason reason
    ) {
        return new BaristaApiException(status, reason);
    }

    private record FinancialSnapshot(
            BigDecimal creditValue,
            BigDecimal memberValue,
            BigDecimal payoutRate,
            BigDecimal payoutAmount,
            BigDecimal margin
    ) {
    }
}
