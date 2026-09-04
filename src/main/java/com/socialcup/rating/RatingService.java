package com.socialcup.rating;

import com.socialcup.drink.Drink;
import com.socialcup.drink.DrinkRepository;
import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RatingService {

    private final DrinkRatingRepository drinkRatingRepository;
    private final DrinkRepository drinkRepository;
    private final UserRepository userRepository;

    public RatingService(
            DrinkRatingRepository drinkRatingRepository,
            DrinkRepository drinkRepository,
            UserRepository userRepository
    ) {
        this.drinkRatingRepository = drinkRatingRepository;
        this.drinkRepository = drinkRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RatingResponse setRating(Long userId, Long drinkId, RatingRequest request) {
        User user = getUser(userId);
        Drink drink = getDrink(drinkId);
        String note = normalizeNote(request.note());

        Optional<DrinkRating> existingRating = drinkRatingRepository
                .findByUserIdAndDrinkId(userId, drinkId);
        DrinkRating rating;
        if (existingRating.isPresent()) {
            rating = existingRating.get();
            rating.update(request.stars(), note);
        } else {
            rating = DrinkRating.create(user, drink, request.stars(), note);
        }

        return toRatingResponse(drinkRatingRepository.save(rating));
    }

    @Transactional(readOnly = true)
    public RatingResponse getMyRating(Long userId, Long drinkId) {
        ensureDrinkExists(drinkId);
        DrinkRating rating = drinkRatingRepository.findByUserIdAndDrinkId(userId, drinkId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Rating not found"
                ));
        return toRatingResponse(rating);
    }

    @Transactional(readOnly = true)
    public RatingSummaryResponse getDrinkRatingSummary(Long drinkId) {
        ensureDrinkExists(drinkId);
        return toRatingSummary(drinkRatingRepository.findDrinkRatingAggregate(drinkId));
    }

    @Transactional(readOnly = true)
    public List<DrinkDiaryResponse> getDrinkDiary(Long userId) {
        return drinkRatingRepository.findDiaryByUserId(userId)
                .stream()
                .map(this::toDiaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, RatingSummaryResponse> getCafeRatingSummaries(Collection<Long> cafeIds) {
        if (cafeIds.isEmpty()) {
            return Map.of();
        }
        return drinkRatingRepository.findCafeRatingAggregates(cafeIds)
                .stream()
                .collect(Collectors.toMap(
                        CafeRatingAggregate::cafeId,
                        aggregate -> new RatingSummaryResponse(
                                roundAverage(aggregate.averageRating()),
                                aggregate.ratingCount()
                        )
                ));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Drink getDrink(Long drinkId) {
        return drinkRepository.findById(drinkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Drink not found"));
    }

    private void ensureDrinkExists(Long drinkId) {
        if (!drinkRepository.existsById(drinkId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Drink not found");
        }
    }

    private RatingResponse toRatingResponse(DrinkRating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getDrink().getId(),
                rating.getStars(),
                rating.getNote(),
                rating.getCreatedAt(),
                rating.getUpdatedAt()
        );
    }

    private RatingSummaryResponse toRatingSummary(RatingAggregate aggregate) {
        if (aggregate == null || aggregate.ratingCount() == 0) {
            return RatingSummaryResponse.empty();
        }
        return new RatingSummaryResponse(
                roundAverage(aggregate.averageRating()),
                aggregate.ratingCount()
        );
    }

    private DrinkDiaryResponse toDiaryResponse(DrinkRating rating) {
        Drink drink = rating.getDrink();
        return new DrinkDiaryResponse(
                rating.getId(),
                drink.getId(),
                drink.getName(),
                drink.getCafe().getId(),
                drink.getCafe().getName(),
                rating.getStars(),
                rating.getNote(),
                rating.getCreatedAt()
        );
    }

    private BigDecimal roundAverage(Double average) {
        if (average == null) {
            return null;
        }
        return BigDecimal.valueOf(average)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    private String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        return note.trim();
    }
}
