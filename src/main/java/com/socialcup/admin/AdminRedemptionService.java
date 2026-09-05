package com.socialcup.admin;

import com.socialcup.redemption.Redemption;
import com.socialcup.redemption.RedemptionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminRedemptionService {

    private static final int MAX_LIMIT = 200;

    private final RedemptionRepository redemptionRepository;

    public AdminRedemptionService(RedemptionRepository redemptionRepository) {
        this.redemptionRepository = redemptionRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminRedemptionResponse> getRecentRedemptions(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Limit must be between 1 and " + MAX_LIMIT
            );
        }

        return redemptionRepository
                .findAllByOrderByRedeemedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AdminRedemptionResponse toResponse(Redemption redemption) {
        return new AdminRedemptionResponse(
                redemption.getId(),
                redemption.getRedeemedAt(),
                redemption.getMember().getDisplayName(),
                redemption.getCafe().getId(),
                redemption.getCafe().getName(),
                redemption.getDrink().getId(),
                redemption.getDrink().getName(),
                redemption.getCreditsSpent(),
                redemption.getStatus(),
                redemption.getPayoutAmount()
        );
    }
}
