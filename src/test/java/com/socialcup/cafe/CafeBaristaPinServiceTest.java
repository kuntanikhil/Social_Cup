package com.socialcup.cafe;

import com.socialcup.barista.CafeDevice;
import com.socialcup.barista.CafeDeviceRepository;
import com.socialcup.drink.DrinkRepository;
import com.socialcup.neighbourhood.NeighbourhoodRepository;
import com.socialcup.rating.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CafeBaristaPinServiceTest {

    @Mock
    private CafeRepository cafeRepository;
    @Mock
    private CafePhotoRepository cafePhotoRepository;
    @Mock
    private CafeOpeningHoursRepository cafeOpeningHoursRepository;
    @Mock
    private DrinkRepository drinkRepository;
    @Mock
    private NeighbourhoodRepository neighbourhoodRepository;
    @Mock
    private RatingService ratingService;
    @Mock
    private CafeDeviceRepository cafeDeviceRepository;
    @Mock
    private Cafe cafe;
    @Mock
    private CafeDevice firstDevice;
    @Mock
    private CafeDevice secondDevice;

    private PasswordEncoder passwordEncoder;
    private CafeService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new CafeService(
                cafeRepository,
                cafePhotoRepository,
                cafeOpeningHoursRepository,
                drinkRepository,
                neighbourhoodRepository,
                ratingService,
                cafeDeviceRepository,
                passwordEncoder
        );
    }

    @Test
    void updatesBcryptPinAndRevokesExistingDevices() {
        when(cafeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(cafe));
        when(cafe.getId()).thenReturn(1L);
        when(cafeDeviceRepository.findActiveByCafeIdForUpdate(1L))
                .thenReturn(List.of(firstDevice, secondDevice));

        BaristaPinUpdateResponse response = service.updateBaristaPin(
                1L,
                new BaristaPinUpdateRequest("1234")
        );

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(cafe).updateBaristaPinHash(hash.capture());
        assertNotEquals("1234", hash.getValue());
        assertTrue(passwordEncoder.matches("1234", hash.getValue()));
        verify(firstDevice).revoke(any());
        verify(secondDevice).revoke(any());
        assertEquals(1L, response.cafeId());
        assertEquals("Barista PIN updated successfully", response.message());
    }

    @Test
    void rejectsMalformedPinBeforeLoadingCafe() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateBaristaPin(
                        1L,
                        new BaristaPinUpdateRequest("12ab")
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(cafeRepository, never()).findByIdForUpdate(any());
        verify(cafeDeviceRepository, never()).findActiveByCafeIdForUpdate(any());
    }
}
