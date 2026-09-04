package com.socialcup.cafe;

import com.socialcup.drink.Drink;
import com.socialcup.drink.DrinkRepository;
import com.socialcup.drink.DrinkResponse;
import com.socialcup.neighbourhood.Neighbourhood;
import com.socialcup.neighbourhood.NeighbourhoodRepository;
import com.socialcup.neighbourhood.NeighbourhoodResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class CafeService {

    private final CafeRepository cafeRepository;
    private final CafePhotoRepository cafePhotoRepository;
    private final CafeOpeningHoursRepository cafeOpeningHoursRepository;
    private final DrinkRepository drinkRepository;
    private final NeighbourhoodRepository neighbourhoodRepository;

    public CafeService(
            CafeRepository cafeRepository,
            CafePhotoRepository cafePhotoRepository,
            CafeOpeningHoursRepository cafeOpeningHoursRepository,
            DrinkRepository drinkRepository,
            NeighbourhoodRepository neighbourhoodRepository
    ) {
        this.cafeRepository = cafeRepository;
        this.cafePhotoRepository = cafePhotoRepository;
        this.cafeOpeningHoursRepository = cafeOpeningHoursRepository;
        this.drinkRepository = drinkRepository;
        this.neighbourhoodRepository = neighbourhoodRepository;
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

    @Transactional(readOnly = true)
    public List<CafeResponse> getAllCafesForAdmin() {
        return cafeRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CafeCardResponse> getActiveCafes() {
        return cafeRepository.findByActiveTrueOrderByFeaturedDescNameAsc()
                .stream()
                .map(cafe -> new CafeCardResponse(
                        cafe.getId(),
                        cafe.getName(),
                        cafe.getNeighbourhood().getName(),
                        cafe.getAddress(),
                        cafe.getPerkLine(),
                        cafe.isFeatured(),
                        cafe.getLatitude(),
                        cafe.getLongitude(),
                        drinkRepository.findMinimumActiveCreditPriceByCafeId(cafe.getId()).orElse(null)
                ))
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
}
