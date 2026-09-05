package com.socialcup.barista;

import com.socialcup.cafe.Cafe;
import com.socialcup.cafe.CafeRepository;
import com.socialcup.security.SecureTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class CafeDeviceService {

    private static final int MAX_DEVICE_TOKEN_LENGTH = 255;

    private final CafeRepository cafeRepository;
    private final CafeDeviceRepository cafeDeviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureTokenService secureTokenService;

    public CafeDeviceService(
            CafeRepository cafeRepository,
            CafeDeviceRepository cafeDeviceRepository,
            PasswordEncoder passwordEncoder,
            SecureTokenService secureTokenService
    ) {
        this.cafeRepository = cafeRepository;
        this.cafeDeviceRepository = cafeDeviceRepository;
        this.passwordEncoder = passwordEncoder;
        this.secureTokenService = secureTokenService;
    }

    @Transactional
    public CafeDeviceAuthenticationResponse authenticate(
            CafeDeviceAuthenticationRequest request
    ) {
        Cafe cafe = cafeRepository.findById(request.cafeId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cafe not found"
                ));
        if (!cafe.isActive()) {
            throw unauthorized();
        }
        if (cafe.getPinHash() == null
                || !passwordEncoder.matches(request.pin(), cafe.getPinHash())) {
            throw unauthorized();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String rawToken = secureTokenService.generateToken();
        cafeDeviceRepository.save(CafeDevice.create(
                cafe,
                secureTokenService.hash(rawToken),
                now
        ));

        // The applied schema has revocation but no device-expiry column.
        return new CafeDeviceAuthenticationResponse(
                cafe.getId(),
                cafe.getName(),
                rawToken,
                null
        );
    }

    @Transactional
    public AuthenticatedCafeDevice authenticateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()
                || rawToken.length() > MAX_DEVICE_TOKEN_LENGTH) {
            throw unauthorized();
        }

        CafeDevice device = cafeDeviceRepository
                .findByTokenHashAndRevokedAtIsNull(
                        secureTokenService.hash(rawToken)
                )
                .orElseThrow(this::unauthorized);
        if (!device.getCafe().isActive()) {
            throw unauthorized();
        }

        device.markUsed(OffsetDateTime.now(ZoneOffset.UTC));
        return new AuthenticatedCafeDevice(
                device.getId(),
                device.getCafe().getId()
        );
    }

    @Transactional
    public CafeDevice requireValidDeviceForUpdate(
            AuthenticatedCafeDevice principal
    ) {
        CafeDevice device = cafeDeviceRepository
                .findValidByIdForUpdate(principal.deviceId())
                .orElseThrow(this::unauthorized);
        if (!device.getCafe().isActive()
                || !device.getCafe().getId().equals(principal.cafeId())) {
            throw unauthorized();
        }
        return device;
    }

    @Transactional(readOnly = true)
    public CafeDevice requireValidDevice(AuthenticatedCafeDevice principal) {
        CafeDevice device = cafeDeviceRepository
                .findById(principal.deviceId())
                .filter(candidate -> candidate.getRevokedAt() == null)
                .orElseThrow(this::unauthorized);
        if (!device.getCafe().isActive()
                || !device.getCafe().getId().equals(principal.cafeId())) {
            throw unauthorized();
        }
        return device;
    }

    private BaristaApiException unauthorized() {
        return new BaristaApiException(
                HttpStatus.UNAUTHORIZED,
                BaristaFailureReason.DEVICE_UNAUTHORIZED
        );
    }
}
