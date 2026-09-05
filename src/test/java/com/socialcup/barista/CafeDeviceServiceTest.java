package com.socialcup.barista;

import com.socialcup.cafe.Cafe;
import com.socialcup.cafe.CafeRepository;
import com.socialcup.security.SecureTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CafeDeviceServiceTest {

    @Mock
    private CafeRepository cafeRepository;
    @Mock
    private CafeDeviceRepository cafeDeviceRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecureTokenService secureTokenService;
    @Mock
    private Cafe cafe;

    private CafeDeviceService service;

    @BeforeEach
    void setUp() {
        service = new CafeDeviceService(
                cafeRepository,
                cafeDeviceRepository,
                passwordEncoder,
                secureTokenService
        );
    }

    @Test
    void validPinCreatesHashedDeviceToken() {
        when(cafeRepository.findById(1L)).thenReturn(Optional.of(cafe));
        when(cafe.isActive()).thenReturn(true);
        when(cafe.getPinHash()).thenReturn("pin-hash");
        when(cafe.getId()).thenReturn(1L);
        when(cafe.getName()).thenReturn("Social Brew Uptown");
        when(passwordEncoder.matches("1234", "pin-hash")).thenReturn(true);
        when(secureTokenService.generateToken()).thenReturn("raw-device-token");
        when(secureTokenService.hash("raw-device-token")).thenReturn("token-hash");

        CafeDeviceAuthenticationResponse response = service.authenticate(
                new CafeDeviceAuthenticationRequest(1L, "1234")
        );

        assertEquals("raw-device-token", response.deviceToken());
        assertEquals(1L, response.cafeId());
        assertNull(response.expiresAt());
        verify(cafeDeviceRepository).save(any(CafeDevice.class));
    }

    @Test
    void invalidPinDoesNotCreateDevice() {
        when(cafeRepository.findById(1L)).thenReturn(Optional.of(cafe));
        when(cafe.isActive()).thenReturn(true);
        when(cafe.getPinHash()).thenReturn("pin-hash");
        when(passwordEncoder.matches("9999", "pin-hash")).thenReturn(false);

        BaristaApiException exception = assertThrows(
                BaristaApiException.class,
                () -> service.authenticate(
                        new CafeDeviceAuthenticationRequest(1L, "9999")
                )
        );

        assertEquals(BaristaFailureReason.DEVICE_UNAUTHORIZED, exception.getReason());
        verify(cafeDeviceRepository, never()).save(any());
    }
}
