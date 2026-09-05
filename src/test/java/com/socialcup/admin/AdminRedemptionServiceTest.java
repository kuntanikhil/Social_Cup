package com.socialcup.admin;

import com.socialcup.cafe.Cafe;
import com.socialcup.drink.Drink;
import com.socialcup.redemption.Redemption;
import com.socialcup.redemption.RedemptionRepository;
import com.socialcup.redemption.RedemptionStatus;
import com.socialcup.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRedemptionServiceTest {

    @Mock
    private RedemptionRepository redemptionRepository;
    @Mock
    private Redemption redemption;
    @Mock
    private User member;
    @Mock
    private Cafe cafe;
    @Mock
    private Drink drink;

    private AdminRedemptionService service;

    @BeforeEach
    void setUp() {
        service = new AdminRedemptionService(redemptionRepository);
    }

    @Test
    void returnsLimitedRecentOperationalAndPayoutFields() {
        OffsetDateTime redeemedAt = OffsetDateTime.parse("2026-09-05T12:00:00Z");
        when(redemptionRepository.findAllByOrderByRedeemedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(redemption));
        when(redemption.getId()).thenReturn(10L);
        when(redemption.getRedeemedAt()).thenReturn(redeemedAt);
        when(redemption.getMember()).thenReturn(member);
        when(member.getDisplayName()).thenReturn("Nikhil");
        when(redemption.getCafe()).thenReturn(cafe);
        when(cafe.getId()).thenReturn(1L);
        when(cafe.getName()).thenReturn("Social Brew Uptown");
        when(redemption.getDrink()).thenReturn(drink);
        when(drink.getId()).thenReturn(4L);
        when(drink.getName()).thenReturn("Vanilla Cloud Latte");
        when(redemption.getCreditsSpent()).thenReturn(5);
        when(redemption.getStatus()).thenReturn(RedemptionStatus.COMPLETED);
        when(redemption.getPayoutAmount()).thenReturn(new BigDecimal("3.25"));

        AdminRedemptionResponse response = service.getRecentRedemptions(25).getFirst();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(redemptionRepository).findAllByOrderByRedeemedAtDesc(pageable.capture());
        assertEquals(25, pageable.getValue().getPageSize());
        assertEquals(10L, response.redemptionId());
        assertEquals("Nikhil", response.memberName());
        assertEquals("Social Brew Uptown", response.cafeName());
        assertEquals("Vanilla Cloud Latte", response.drinkName());
        assertEquals(new BigDecimal("3.25"), response.payoutAmount());
    }

    @Test
    void rejectsLimitsOutsideSafeRecentWindow() {
        ResponseStatusException tooSmall = assertThrows(
                ResponseStatusException.class,
                () -> service.getRecentRedemptions(0)
        );
        ResponseStatusException tooLarge = assertThrows(
                ResponseStatusException.class,
                () -> service.getRecentRedemptions(201)
        );

        assertEquals(HttpStatus.BAD_REQUEST, tooSmall.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, tooLarge.getStatusCode());
        verify(redemptionRepository, never()).findAllByOrderByRedeemedAtDesc(any());
    }
}
