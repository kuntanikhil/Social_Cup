package com.socialcup.drink;

import com.socialcup.cafe.Cafe;
import com.socialcup.cafe.CafeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DrinkService {

    private final DrinkRepository drinkRepository;
    private final CafeRepository cafeRepository;

    public DrinkService(DrinkRepository drinkRepository, CafeRepository cafeRepository) {
        this.drinkRepository = drinkRepository;
        this.cafeRepository = cafeRepository;
    }

    @Transactional
    public AdminDrinkResponse createDrink(Long cafeId, DrinkCreateRequest request) {
        Cafe cafe = getCafe(cafeId);
        Drink drink = Drink.create(
                cafe,
                request.name(),
                request.type(),
                request.description(),
                request.photoPath(),
                request.retailPrice(),
                request.creditPrice(),
                request.signature()
        );
        return toAdminResponse(drinkRepository.save(drink));
    }

    @Transactional
    public AdminDrinkResponse updateDrink(Long id, DrinkUpdateRequest request) {
        Drink drink = drinkRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Drink not found"));
        drink.update(
                request.name(),
                request.type(),
                request.description(),
                request.photoPath(),
                request.retailPrice(),
                request.creditPrice(),
                request.signature(),
                request.active()
        );
        return toAdminResponse(drinkRepository.save(drink));
    }

    @Transactional(readOnly = true)
    public List<AdminDrinkResponse> getDrinksForAdmin(Long cafeId) {
        getCafe(cafeId);
        return drinkRepository.findByCafeIdOrderByNameAsc(cafeId)
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    private Cafe getCafe(Long id) {
        return cafeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cafe not found"));
    }

    private AdminDrinkResponse toAdminResponse(Drink drink) {
        return new AdminDrinkResponse(
                drink.getId(),
                drink.getCafe().getId(),
                drink.getName(),
                drink.getType(),
                drink.getDescription(),
                drink.getPhotoPath(),
                drink.getRetailPrice(),
                drink.getCreditPrice(),
                drink.isSignature(),
                drink.isActive(),
                drink.getCreatedAt(),
                drink.getUpdatedAt()
        );
    }
}
