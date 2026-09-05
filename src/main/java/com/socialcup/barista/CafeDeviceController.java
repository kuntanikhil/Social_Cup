package com.socialcup.barista;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/barista/device")
public class CafeDeviceController {

    private final CafeDeviceService cafeDeviceService;

    public CafeDeviceController(CafeDeviceService cafeDeviceService) {
        this.cafeDeviceService = cafeDeviceService;
    }

    @PostMapping("/authenticate")
    public CafeDeviceAuthenticationResponse authenticate(
            @Valid @RequestBody CafeDeviceAuthenticationRequest request
    ) {
        return cafeDeviceService.authenticate(request);
    }
}
