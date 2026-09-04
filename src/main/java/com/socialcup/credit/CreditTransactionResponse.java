package com.socialcup.credit;

import java.time.OffsetDateTime;

public record CreditTransactionResponse(
        CreditTransactionType type,
        Integer amount,
        OffsetDateTime createdAt
) {
}
