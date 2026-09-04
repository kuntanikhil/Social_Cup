package com.socialcup.rating;

public record RatingAggregate(
        Double averageRating,
        Long ratingCount
) {
}
