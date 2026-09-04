package com.socialcup.discovery;

import com.socialcup.cafe.Cafe;
import com.socialcup.cafe.CafeRepository;
import com.socialcup.drink.Drink;
import com.socialcup.drink.DrinkRepository;
import com.socialcup.neighbourhood.NeighbourhoodRepository;
import com.socialcup.profile.CoffeePreference;
import com.socialcup.profile.UserCoffeePreferenceRepository;
import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DiscoverService {

    private static final double EARTH_RADIUS_KM = 6_371.0088;

    private final UserRepository userRepository;
    private final UserCoffeePreferenceRepository userCoffeePreferenceRepository;
    private final NeighbourhoodRepository neighbourhoodRepository;
    private final CafeRepository cafeRepository;
    private final DrinkRepository drinkRepository;

    public DiscoverService(
            UserRepository userRepository,
            UserCoffeePreferenceRepository userCoffeePreferenceRepository,
            NeighbourhoodRepository neighbourhoodRepository,
            CafeRepository cafeRepository,
            DrinkRepository drinkRepository
    ) {
        this.userRepository = userRepository;
        this.userCoffeePreferenceRepository = userCoffeePreferenceRepository;
        this.neighbourhoodRepository = neighbourhoodRepository;
        this.cafeRepository = cafeRepository;
        this.drinkRepository = drinkRepository;
    }

    @Transactional(readOnly = true)
    public DiscoverResponse discover(
            Long userId,
            BigDecimal latitude,
            BigDecimal longitude,
            String search,
            Long neighbourhoodId
    ) {
        validateCoordinates(latitude, longitude);
        validateNeighbourhood(neighbourhoodId);

        User user = userRepository.findProfileById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        String normalizedSearch = normalizeSearch(search);
        List<Cafe> cafes = cafeRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .filter(cafe -> matchesNeighbourhood(cafe, neighbourhoodId))
                .filter(cafe -> matchesSearch(cafe, normalizedSearch))
                .toList();

        List<Drink> activeDrinks = loadActiveDrinks(cafes);
        Map<Long, List<Drink>> drinksByCafeId = activeDrinks.stream()
                .collect(Collectors.groupingBy(drink -> drink.getCafe().getId()));
        Set<String> preferenceCodes = loadActivePreferenceCodes(userId);
        boolean hasLocation = latitude != null && longitude != null;

        List<CafeDiscoveryData> rankedCafes = new ArrayList<>(cafes.size());
        for (Cafe cafe : cafes) {
            List<Drink> cafeDrinks = drinksByCafeId.getOrDefault(cafe.getId(), List.of());
            rankedCafes.add(new CafeDiscoveryData(
                    cafe,
                    minimumCreditPrice(cafeDrinks),
                    calculateDistance(latitude, longitude, cafe, hasLocation),
                    isPreferenceMatch(cafeDrinks, preferenceCodes)
            ));
        }
        rankedCafes.sort(cafeComparator(user, hasLocation));

        Map<Long, Integer> cafeRankById = new HashMap<>();
        for (int index = 0; index < rankedCafes.size(); index++) {
            cafeRankById.put(rankedCafes.get(index).cafe().getId(), index);
        }

        List<FeaturedCafeResponse> featuredCafes = rankedCafes.stream()
                .filter(data -> data.cafe().isFeatured())
                .map(this::toFeaturedCafeResponse)
                .toList();

        List<SignatureDrinkResponse> signatureDrinks = activeDrinks.stream()
                .filter(Drink::isSignature)
                .sorted(signatureDrinkComparator(cafeRankById))
                .map(this::toSignatureDrinkResponse)
                .toList();

        List<DiscoverCafeResponse> cafeResponses = rankedCafes.stream()
                .map(this::toDiscoverCafeResponse)
                .toList();

        return new DiscoverResponse(featuredCafes, signatureDrinks, cafeResponses);
    }

    private List<Drink> loadActiveDrinks(List<Cafe> cafes) {
        if (cafes.isEmpty()) {
            return List.of();
        }
        List<Long> cafeIds = cafes.stream()
                .map(Cafe::getId)
                .toList();
        return drinkRepository.findActiveDrinksForActiveCafes(cafeIds);
    }

    private Set<String> loadActivePreferenceCodes(Long userId) {
        return userCoffeePreferenceRepository.findCoffeePreferencesByUserId(userId)
                .stream()
                .filter(CoffeePreference::isActive)
                .map(CoffeePreference::getCode)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Comparator<CafeDiscoveryData> cafeComparator(User user, boolean hasLocation) {
        Comparator<CafeDiscoveryData> comparator = Comparator.comparingInt(this::rankingGroup);
        if (hasLocation) {
            comparator = comparator.thenComparing(
                    CafeDiscoveryData::distanceKm,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
        } else {
            Long homeNeighbourhoodId = user.getHomeNeighbourhood() == null
                    ? null
                    : user.getHomeNeighbourhood().getId();
            comparator = comparator.thenComparing(data ->
                    !Objects.equals(data.cafe().getNeighbourhood().getId(), homeNeighbourhoodId));
        }
        return comparator
                .thenComparing(data -> data.cafe().getName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(data -> data.cafe().getId());
    }

    private Comparator<Drink> signatureDrinkComparator(Map<Long, Integer> cafeRankById) {
        return Comparator
                .comparingInt((Drink drink) ->
                        cafeRankById.getOrDefault(drink.getCafe().getId(), Integer.MAX_VALUE))
                .thenComparing(Drink::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Drink::getId);
    }

    private int rankingGroup(CafeDiscoveryData data) {
        if (data.cafe().isFeatured()) {
            return 0;
        }
        return data.preferenceMatch() ? 1 : 2;
    }

    private boolean matchesNeighbourhood(Cafe cafe, Long neighbourhoodId) {
        return neighbourhoodId == null
                || cafe.getNeighbourhood().getId().equals(neighbourhoodId);
    }

    private boolean matchesSearch(Cafe cafe, String normalizedSearch) {
        return normalizedSearch == null
                || cafe.getName().toLowerCase(Locale.ROOT).contains(normalizedSearch);
    }

    private Integer minimumCreditPrice(List<Drink> drinks) {
        return drinks.stream()
                .map(Drink::getCreditPrice)
                .min(Integer::compareTo)
                .orElse(null);
    }

    private boolean isPreferenceMatch(List<Drink> drinks, Set<String> preferenceCodes) {
        return !preferenceCodes.isEmpty()
                && drinks.stream()
                .anyMatch(drink -> preferenceCodes.contains(drink.getType().name()));
    }

    private BigDecimal calculateDistance(
            BigDecimal latitude,
            BigDecimal longitude,
            Cafe cafe,
            boolean hasLocation
    ) {
        if (!hasLocation || cafe.getLatitude() == null || cafe.getLongitude() == null) {
            return null;
        }

        double originLatitude = Math.toRadians(latitude.doubleValue());
        double cafeLatitude = Math.toRadians(cafe.getLatitude().doubleValue());
        double latitudeDelta = cafeLatitude - originLatitude;
        double longitudeDelta = Math.toRadians(
                cafe.getLongitude().subtract(longitude).doubleValue()
        );

        double sinLatitude = Math.sin(latitudeDelta / 2.0);
        double sinLongitude = Math.sin(longitudeDelta / 2.0);
        double haversine = sinLatitude * sinLatitude
                + Math.cos(originLatitude)
                * Math.cos(cafeLatitude)
                * sinLongitude
                * sinLongitude;
        double angularDistance = 2.0 * Math.asin(Math.sqrt(Math.min(1.0, haversine)));

        return BigDecimal.valueOf(EARTH_RADIUS_KM * angularDistance)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude != null
                && (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Latitude must be between -90 and 90"
            );
        }
        if (longitude != null
                && (longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Longitude must be between -180 and 180"
            );
        }
    }

    private void validateNeighbourhood(Long neighbourhoodId) {
        if (neighbourhoodId != null && !neighbourhoodRepository.existsById(neighbourhoodId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Neighbourhood not found");
        }
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim().toLowerCase(Locale.ROOT);
    }

    private FeaturedCafeResponse toFeaturedCafeResponse(CafeDiscoveryData data) {
        Cafe cafe = data.cafe();
        return new FeaturedCafeResponse(
                cafe.getId(),
                cafe.getName(),
                cafe.getNeighbourhood().getName(),
                cafe.getPerkLine(),
                data.minimumCreditPrice()
        );
    }

    private SignatureDrinkResponse toSignatureDrinkResponse(Drink drink) {
        return new SignatureDrinkResponse(
                drink.getId(),
                drink.getName(),
                drink.getType(),
                drink.getCreditPrice(),
                drink.getPhotoPath(),
                new SignatureDrinkCafeResponse(
                        drink.getCafe().getId(),
                        drink.getCafe().getName()
                )
        );
    }

    private DiscoverCafeResponse toDiscoverCafeResponse(CafeDiscoveryData data) {
        Cafe cafe = data.cafe();
        return new DiscoverCafeResponse(
                cafe.getId(),
                cafe.getName(),
                cafe.getNeighbourhood().getName(),
                cafe.getAddress(),
                cafe.getPerkLine(),
                cafe.isFeatured(),
                data.minimumCreditPrice(),
                data.distanceKm(),
                data.preferenceMatch()
        );
    }

    private record CafeDiscoveryData(
            Cafe cafe,
            Integer minimumCreditPrice,
            BigDecimal distanceKm,
            boolean preferenceMatch
    ) {
    }
}
