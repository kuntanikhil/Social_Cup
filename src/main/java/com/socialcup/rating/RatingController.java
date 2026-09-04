package com.socialcup.rating;

import com.socialcup.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PutMapping("/api/drinks/{drinkId}/rating")
    public RatingResponse setRating(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long drinkId,
            @Valid @RequestBody RatingRequest request
    ) {
        return ratingService.setRating(authenticatedUser.id(), drinkId, request);
    }

    @GetMapping("/api/drinks/{drinkId}/my-rating")
    public RatingResponse getMyRating(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long drinkId
    ) {
        return ratingService.getMyRating(authenticatedUser.id(), drinkId);
    }

    @GetMapping("/api/drinks/{drinkId}/rating-summary")
    public RatingSummaryResponse getRatingSummary(@PathVariable Long drinkId) {
        return ratingService.getDrinkRatingSummary(drinkId);
    }

    @GetMapping("/api/profile/drink-diary")
    public List<DrinkDiaryResponse> getDrinkDiary(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ratingService.getDrinkDiary(authenticatedUser.id());
    }
}
