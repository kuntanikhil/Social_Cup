package com.socialcup.redemption;

import com.socialcup.cafe.Cafe;
import com.socialcup.cafe.CafeRepository;
import com.socialcup.credit.CreditService;
import com.socialcup.drink.Drink;
import com.socialcup.drink.DrinkRepository;
import com.socialcup.membership.Subscription;
import com.socialcup.membership.SubscriptionRepository;
import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Service
public class RedemptionSessionService {

    private static final Duration SESSION_LIFETIME = Duration.ofMinutes(5);
    private static final String API_PENDING_STATUS = "PENDING";

    private final UserRepository userRepository;
    private final CafeRepository cafeRepository;
    private final DrinkRepository drinkRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CreditService creditService;
    private final RedemptionSessionRepository redemptionSessionRepository;
    private final RedemptionCredentialGenerator credentialGenerator;

    public RedemptionSessionService(
            UserRepository userRepository,
            CafeRepository cafeRepository,
            DrinkRepository drinkRepository,
            SubscriptionRepository subscriptionRepository,
            CreditService creditService,
            RedemptionSessionRepository redemptionSessionRepository,
            RedemptionCredentialGenerator credentialGenerator
    ) {
        this.userRepository = userRepository;
        this.cafeRepository = cafeRepository;
        this.drinkRepository = drinkRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.creditService = creditService;
        this.redemptionSessionRepository = redemptionSessionRepository;
        this.credentialGenerator = credentialGenerator;
    }

    @Transactional
    public CreateRedemptionSessionResponse create(
            Long userId,
            Long cafeId,
            CreateRedemptionSessionRequest request
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        User member = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> notFound("User not found"));
        Cafe cafe = cafeRepository.findByIdAndActiveTrue(cafeId)
                .orElseThrow(() -> notFound("Active cafe not found"));
        Drink drink = drinkRepository.findByIdAndActiveTrue(request.drinkId())
                .orElseThrow(() -> notFound("Active drink not found"));

        if (!Objects.equals(drink.getCafe().getId(), cafe.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Drink does not belong to the selected cafe"
            );
        }

        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> forbidden("An active membership is required"));
        validateMembership(subscription, now);

        int creditCost = drink.getCreditPrice();
        int creditsBefore = creditService.getBalance(member);
        if (creditsBefore < creditCost) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient credits for this drink"
            );
        }

        invalidateExistingSessions(member.getId(), now);

        RedemptionCredentialGenerator.GeneratedRedemptionCredentials credentials =
                credentialGenerator.generate();
        OffsetDateTime expiresAt = now.plus(SESSION_LIFETIME);
        RedemptionSession session = redemptionSessionRepository.save(
                RedemptionSession.create(
                        member,
                        cafe,
                        drink,
                        creditCost,
                        credentials.qrTokenHash(),
                        credentials.backupCodeHash(),
                        now,
                        expiresAt
                )
        );

        return new CreateRedemptionSessionResponse(
                session.getId(),
                cafeResponse(cafe),
                drinkResponse(drink, creditCost),
                creditsBefore,
                creditsBefore - creditCost,
                credentials.qrToken(),
                credentials.backupCode(),
                expiresAt,
                SESSION_LIFETIME.toSeconds(),
                API_PENDING_STATUS
        );
    }

    @Transactional
    public RedemptionSessionResponse get(Long userId, Long sessionId) {
        RedemptionSession session = redemptionSessionRepository.findDetailsById(sessionId)
                .orElseThrow(() -> notFound("Redemption session not found"));

        if (!Objects.equals(session.getMember().getId(), userId)) {
            throw forbidden("You cannot access this redemption session");
        }

        OffsetDateTime serverTime = OffsetDateTime.now(ZoneOffset.UTC);
        if (session.getStatus() == RedemptionSessionStatus.ACTIVE
                && session.isExpiredAt(serverTime)) {
            session.expire();
        }

        return new RedemptionSessionResponse(
                session.getId(),
                cafeResponse(session.getCafe()),
                new RedemptionSessionDrinkResponse(
                        session.getDrink().getId(),
                        session.getDrink().getName()
                ),
                session.getCreditCost(),
                apiStatus(session.getStatus()),
                session.getExpiresAt(),
                serverTime
        );
    }

    private void validateMembership(Subscription subscription, OffsetDateTime now) {
        boolean periodHasStarted = subscription.getCurrentPeriodStart() == null
                || !subscription.getCurrentPeriodStart().isAfter(now);
        boolean periodIsCurrent = subscription.getCurrentPeriodEnd() != null
                && subscription.getCurrentPeriodEnd().isAfter(now);

        if (!subscription.getStatus().isMember() || !periodHasStarted || !periodIsCurrent) {
            throw forbidden("An active paid membership is required");
        }
    }

    private void invalidateExistingSessions(Long memberId, OffsetDateTime now) {
        redemptionSessionRepository.findByMemberIdAndStatus(
                        memberId,
                        RedemptionSessionStatus.ACTIVE
                )
                .forEach(existing -> {
                    if (existing.isExpiredAt(now)) {
                        existing.expire();
                    } else {
                        existing.cancel(now);
                    }
                });
    }

    private RedemptionCafeResponse cafeResponse(Cafe cafe) {
        return new RedemptionCafeResponse(cafe.getId(), cafe.getName());
    }

    private RedemptionDrinkResponse drinkResponse(Drink drink, int credits) {
        return new RedemptionDrinkResponse(drink.getId(), drink.getName(), credits);
    }

    private String apiStatus(RedemptionSessionStatus status) {
        return status == RedemptionSessionStatus.ACTIVE
                ? API_PENDING_STATUS
                : status.name();
    }

    private ResponseStatusException notFound(String reason) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
    }

    private ResponseStatusException forbidden(String reason) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, reason);
    }
}
