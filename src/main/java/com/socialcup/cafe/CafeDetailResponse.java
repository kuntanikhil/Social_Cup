package com.socialcup.cafe;

import com.socialcup.drink.DrinkResponse;
import com.socialcup.neighbourhood.NeighbourhoodResponse;

import java.math.BigDecimal;
import java.util.List;

public record CafeDetailResponse(
        Long id,
        String name,
        String address,
        NeighbourhoodResponse neighbourhood,
        BigDecimal latitude,
        BigDecimal longitude,
        String perkLine,
        boolean featured,
        List<CafePhotoResponse> photos,
        List<CafeOpeningHoursResponse> openingHours,
        List<DrinkResponse> drinks
) {
}
