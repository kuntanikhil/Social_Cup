package com.socialcup.cafe;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cafes")
public class CafeController {

    private final CafeService cafeService;

    public CafeController(CafeService cafeService) {
        this.cafeService = cafeService;
    }

    @GetMapping
    public List<CafeCardResponse> getCafes() {
        return cafeService.getActiveCafes();
    }

    @GetMapping("/{id}")
    public CafeDetailResponse getCafe(@PathVariable Long id) {
        return cafeService.getActiveCafe(id);
    }
}
