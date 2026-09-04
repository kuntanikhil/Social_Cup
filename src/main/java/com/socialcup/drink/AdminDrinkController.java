package com.socialcup.drink;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminDrinkController {

    private final DrinkService drinkService;

    public AdminDrinkController(DrinkService drinkService) {
        this.drinkService = drinkService;
    }

    @PostMapping("/cafes/{cafeId}/drinks")
    public ResponseEntity<AdminDrinkResponse> createDrink(
            @PathVariable Long cafeId,
            @Valid @RequestBody DrinkCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(drinkService.createDrink(cafeId, request));
    }

    @PutMapping("/drinks/{id}")
    public AdminDrinkResponse updateDrink(
            @PathVariable Long id,
            @Valid @RequestBody DrinkUpdateRequest request
    ) {
        return drinkService.updateDrink(id, request);
    }

    @GetMapping("/cafes/{cafeId}/drinks")
    public List<AdminDrinkResponse> getDrinks(@PathVariable Long cafeId) {
        return drinkService.getDrinksForAdmin(cafeId);
    }
}
