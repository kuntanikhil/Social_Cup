package com.socialcup.credit;

import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditServiceRedemptionTest {

    @Mock
    private CreditAccountRepository creditAccountRepository;
    @Mock
    private CreditTransactionRepository creditTransactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private User user;

    @Test
    void deductionChangesBalanceAndCreatesOneLedgerEntry() {
        when(user.getId()).thenReturn(1L);
        CreditAccount account = CreditAccount.create(user);
        account.resetTo(30);
        when(creditAccountRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.of(account));
        CreditService service = new CreditService(
                creditAccountRepository,
                creditTransactionRepository,
                userRepository
        );

        int remaining = service.deductForRedemption(user, 50L, 5);

        assertEquals(25, remaining);
        assertEquals(25, account.getCreditsRemaining());
        ArgumentCaptor<CreditTransaction> transaction =
                ArgumentCaptor.forClass(CreditTransaction.class);
        verify(creditTransactionRepository).save(transaction.capture());
        assertEquals(CreditTransactionType.REDEMPTION, transaction.getValue().getType());
        assertEquals(-5, transaction.getValue().getAmount());
        assertEquals(50L, transaction.getValue().getRedemptionId());
    }
}
