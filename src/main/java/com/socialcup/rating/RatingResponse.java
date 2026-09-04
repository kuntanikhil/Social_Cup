package com.socialcup.rating;

import java.time.OffsetDateTime;

public record RatingResponse(
        Long id,
        Long drinkId,
        Integer stars,
        String note,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
