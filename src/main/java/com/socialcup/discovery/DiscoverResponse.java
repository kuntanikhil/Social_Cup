package com.socialcup.discovery;

import java.util.List;

public record DiscoverResponse(
        List<FeaturedCafeResponse> featuredCafes,
        List<SignatureDrinkResponse> signatureDrinks,
        List<DiscoverCafeResponse> cafes
) {
}
