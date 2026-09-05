package com.socialcup.cafe;

import com.socialcup.barista.CafeDevice;
import com.socialcup.barista.CafeDeviceRepository;
import com.socialcup.drink.Drink;
import com.socialcup.drink.DrinkRepository;
import com.socialcup.drink.DrinkResponse;
import com.socialcup.neighbourhood.Neighbourhood;
import com.socialcup.neighbourhood.NeighbourhoodRepository;
import com.socialcup.neighbourhood.NeighbourhoodResponse;
import com.socialcup.rating.RatingService;
import com.socialcup.rating.RatingSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CafeService {

    private final CafeRepository cafeRepository;
    private final CafePhotoRepository cafePhotoRepository;
    private final CafeOpeningHoursRepository cafeOpeningHoursRepository;
    private final DrinkRepository drinkRepository;
    private final NeighbourhoodRepository neighbourhoodRepository;
    private final RatingService ratingService;
    private final CafeDeviceRepository cafeDeviceRepository;
    private final PasswordEncoder passwordEncoder;

    public CafeService(
            CafeRepository cafeRepository,
            CafePhotoRepository cafePhotoRepository,
            CafeOpeningHoursRepository cafeOpeningHoursRepository,
            DrinkRepository drinkRepository,
            NeighbourhoodRepository neighbourhoodRepository,
            RatingService ratingService,
            CafeDeviceRepository cafeDeviceRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.cafeRepository = cafeRepository;
        this.cafePhotoRepository = cafePhotoRepository;
        this.cafeOpeningHoursRepository = cafeOpeningHoursRepository;
        this.drinkRepository = drinkRepository;
        this.neighbourhoodRepository = neighbourhoodRepository;
        this.ratingService = ratingService;
        this.cafeDeviceRepository = cafeDeviceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CafeResponse createCafe(CafeCreateRequest request) {
        Neighbourhood neighbourhood = getNeighbourhood(request.neighbourhoodId());
        Cafe cafe = Cafe.create(
                request.name(),
                request.address(),
                neighbourhood,
                request.latitude(),
                request.longitude(),
                request.perkLine(),
                request.payoutRatePerCredit(),
                request.featured(),
                UUID.randomUUID().toString()
        );
        return toAdminResponse(cafeRepository.save(cafe));
    }

    @Transactional
    public CafeResponse updateCafe(Long id, CafeUpdateRequest request) {
        Cafe cafe = getCafe(id);
        Neighbourhood neighbourhood = getNeighbourhood(request.neighbourhoodId());
        cafe.update(
                request.name(),
                request.address(),
                neighbourhood,
                request.latitude(),
                request.longitude(),
                request.perkLine(),
                request.payoutRatePerCredit(),
                request.featured(),
                request.active()
        );
        return toAdminResponse(cafeRepository.save(cafe));
    }

    @Transactional
    public BaristaPinUpdateResponse updateBaristaPin(
            Long cafeId,
            BaristaPinUpdateRequest request
    ) {
        if (request.pin() == null || !request.pin().matches("\\d{4,6}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "PIN must contain 4 to 6 numeric digits"
            );
        }

        Cafe cafe = cafeRepository.findByIdForUpdate(cafeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cafe not found"
                ));
        cafe.updateBaristaPinHash(passwordEncoder.encode(request.pin()));

        OffsetDateTime revokedAt = OffsetDateTime.now(ZoneOffset.UTC);
        List<CafeDevice> activeDevices = cafeDeviceRepository
                .findActiveByCafeIdForUpdate(cafeId);
        activeDevices.forEach(device -> device.revoke(revokedAt));

        cafeRepository.save(cafe);
        return new BaristaPinUpdateResponse(
                cafe.getId(),
                "Barista PIN updated successfully"
        );
    }

    @Transactional(readOnly = true)
    public List<CafeResponse> getAllCafesForAdmin() {
        return cafeRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CafeCardResponse> getActiveCafes() {
        List<Cafe> cafes = cafeRepository.findByActiveTrueOrderByFeaturedDescNameAsc();
        Map<Long, List<Drink>> drinksByCafeId = getActiveDrinksByCafeId(cafes);
        Map<Long, RatingSummaryResponse> ratingsByCafeId = ratingService
                .getCafeRatingSummaries(cafes.stream().map(Cafe::getId).toList());

        return cafes
                .stream()
                .map(cafe -> {
                    RatingSummaryResponse rating = ratingFor(ratingsByCafeId, cafe.getId());
                    return new CafeCardResponse(
                            cafe.getId(),
                            cafe.getName(),
                            cafe.getNeighbourhood().getName(),
                            cafe.getAddress(),
                            cafe.getPerkLine(),
                            cafe.isFeatured(),
                            cafe.getLatitude(),
                            cafe.getLongitude(),
                            minimumCreditPrice(drinksByCafeId.getOrDefault(cafe.getId(), List.of())),
                            rating.averageRating(),
                            rating.ratingCount(),
                            rating.ratingCount() == 0
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CafeDetailResponse getActiveCafe(Long id) {
        Cafe cafe = cafeRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cafe not found"));

        List<CafePhotoResponse> photos = cafePhotoRepository
                .findByCafeIdOrderByDisplayOrderAscIdAsc(id)
                .stream()
                .map(photo -> new CafePhotoResponse(
                        photo.getId(),
                        photo.getStoragePath(),
                        photo.getDisplayOrder()
                ))
                .toList();

        List<CafeOpeningHoursResponse> openingHours = cafeOpeningHoursRepository
                .findByCafeIdOrderByDayOfWeekAsc(id)
                .stream()
                .map(hours -> new CafeOpeningHoursResponse(
                        hours.getId(),
                        hours.getDayOfWeek(),
                        hours.getOpensAt(),
                        hours.getClosesAt(),
                        hours.isClosed()
                ))
                .toList();

        List<DrinkResponse> drinks = drinkRepository.findByCafeIdAndActiveTrueOrderByNameAsc(id)
                .stream()
                .map(this::toPublicDrinkResponse)
                .toList();
        RatingSummaryResponse rating = ratingFor(
                ratingService.getCafeRatingSummaries(List.of(id)),
                id
        );

        return new CafeDetailResponse(
                cafe.getId(),
                cafe.getName(),
                cafe.getAddress(),
                new NeighbourhoodResponse(
                        cafe.getNeighbourhood().getId(),
                        cafe.getNeighbourhood().getName()
                ),
                cafe.getLatitude(),
                cafe.getLongitude(),
                cafe.getPerkLine(),
                cafe.isFeatured(),
                rating.averageRating(),
                rating.ratingCount(),
                rating.ratingCount() == 0,
                photos,
                openingHours,
                drinks
        );
    }

    private Cafe getCafe(Long id) {
        return cafeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cafe not found"));
    }

    private Neighbourhood getNeighbourhood(Long id) {
        return neighbourhoodRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Neighbourhood not found"
                ));
    }

    private CafeResponse toAdminResponse(Cafe cafe) {
        return new CafeResponse(
                cafe.getId(),
                cafe.getName(),
                cafe.getAddress(),
                cafe.getNeighbourhood().getId(),
                cafe.getNeighbourhood().getName(),
                cafe.getLatitude(),
                cafe.getLongitude(),
                cafe.getPerkLine(),
                cafe.getPayoutRatePerCredit(),
                cafe.isFeatured(),
                cafe.isActive(),
                cafe.getScanSlug(),
                cafe.getCreatedAt(),
                cafe.getUpdatedAt()
        );
    }

    private DrinkResponse toPublicDrinkResponse(Drink drink) {
        return new DrinkResponse(
                drink.getId(),
                drink.getName(),
                drink.getType(),
                drink.getDescription(),
                drink.getPhotoPath(),
                drink.getRetailPrice(),
                drink.getCreditPrice(),
                drink.isSignature()
        );
    }

    private Map<Long, List<Drink>> getActiveDrinksByCafeId(List<Cafe> cafes) {
        if (cafes.isEmpty()) {
            return Map.of();
        }
        return drinkRepository.findActiveDrinksForActiveCafes(
                        cafes.stream().map(Cafe::getId).toList()
                )
                .stream()
                .collect(Collectors.groupingBy(drink -> drink.getCafe().getId()));
    }

    private Integer minimumCreditPrice(List<Drink> drinks) {
        return drinks.stream()
                .map(Drink::getCreditPrice)
                .min(Integer::compareTo)
                .orElse(null);
    }

    private RatingSummaryResponse ratingFor(
            Map<Long, RatingSummaryResponse> ratingsByCafeId,
            Long cafeId
    ) {
        return ratingsByCafeId.getOrDefault(cafeId, RatingSummaryResponse.empty());
    }
}
