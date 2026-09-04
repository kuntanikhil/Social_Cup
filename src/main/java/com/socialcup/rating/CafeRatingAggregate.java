package com.socialcup.rating;

public record CafeRatingAggregate(
        Long cafeId,
        Double averageRating,
        Long ratingCount
) {
}
