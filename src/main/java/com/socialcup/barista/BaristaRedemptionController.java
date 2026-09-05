package com.socialcup.barista;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/barista/redemptions")
public class BaristaRedemptionController {

    private final BaristaRedemptionService baristaRedemptionService;

    public BaristaRedemptionController(
            BaristaRedemptionService baristaRedemptionService
    ) {
        this.baristaRedemptionService = baristaRedemptionService;
    }

    @PostMapping("/validate")
    public BaristaRedemptionResponse validate(
            @AuthenticationPrincipal AuthenticatedCafeDevice device,
            @RequestBody ValidateRedemptionRequest request
    ) {
        return baristaRedemptionService.validate(device, request);
    }

    @GetMapping("/today")
    public List<BaristaTodayRedemptionResponse> today(
            @AuthenticationPrincipal AuthenticatedCafeDevice device
    ) {
        return baristaRedemptionService.today(device);
    }
}
