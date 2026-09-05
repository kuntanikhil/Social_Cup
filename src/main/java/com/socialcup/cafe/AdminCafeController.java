package com.socialcup.cafe;

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
@RequestMapping("/api/admin/cafes")
public class AdminCafeController {

    private final CafeService cafeService;

    public AdminCafeController(CafeService cafeService) {
        this.cafeService = cafeService;
    }

    @PostMapping
    public ResponseEntity<CafeResponse> createCafe(
            @Valid @RequestBody CafeCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cafeService.createCafe(request));
    }

    @PutMapping("/{id}")
    public CafeResponse updateCafe(
            @PathVariable Long id,
            @Valid @RequestBody CafeUpdateRequest request
    ) {
        return cafeService.updateCafe(id, request);
    }

    @PutMapping("/{cafeId}/barista-pin")
    public BaristaPinUpdateResponse updateBaristaPin(
            @PathVariable Long cafeId,
            @Valid @RequestBody BaristaPinUpdateRequest request
    ) {
        return cafeService.updateBaristaPin(cafeId, request);
    }

    @GetMapping
    public List<CafeResponse> getCafes() {
        return cafeService.getAllCafesForAdmin();
    }
}
