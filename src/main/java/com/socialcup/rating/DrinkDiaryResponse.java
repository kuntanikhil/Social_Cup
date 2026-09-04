package com.socialcup.rating;

import java.time.OffsetDateTime;

public record DrinkDiaryResponse(
        Long ratingId,
        Long drinkId,
        String drinkName,
        Long cafeId,
        String cafeName,
        Integer stars,
        String note,
        OffsetDateTime ratedAt
) {
}
