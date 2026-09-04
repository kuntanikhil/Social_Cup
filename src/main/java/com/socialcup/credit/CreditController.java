package com.socialcup.credit;

import com.socialcup.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
public class CreditController {

    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    @GetMapping("/transactions")
    public List<CreditTransactionResponse> getTransactions(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return creditService.getTransactions(authenticatedUser.id());
    }
}
