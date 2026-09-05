package com.socialcup.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/redemptions")
public class AdminRedemptionController {

    private final AdminRedemptionService adminRedemptionService;

    public AdminRedemptionController(AdminRedemptionService adminRedemptionService) {
        this.adminRedemptionService = adminRedemptionService;
    }

    @GetMapping
    public List<AdminRedemptionResponse> getRedemptions(
            @RequestParam(defaultValue = "100") int limit
    ) {
        return adminRedemptionService.getRecentRedemptions(limit);
    }
}
