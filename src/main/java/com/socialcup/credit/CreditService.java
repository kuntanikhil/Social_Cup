package com.socialcup.credit;

import com.socialcup.membership.BillingCycle;
import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CreditService {

    public static final int MONTHLY_CREDIT_BALANCE = 30;

    private final CreditAccountRepository creditAccountRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final UserRepository userRepository;

    public CreditService(
            CreditAccountRepository creditAccountRepository,
            CreditTransactionRepository creditTransactionRepository,
            UserRepository userRepository
    ) {
        this.creditAccountRepository = creditAccountRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CreditAccount getOrCreateCreditAccount(User user) {
        return creditAccountRepository.findById(user.getId())
                .orElseGet(() -> creditAccountRepository.save(CreditAccount.create(user)));
    }

    @Transactional
    public int getBalance(User user) {
        return getOrCreateCreditAccount(user).getCreditsRemaining();
    }

    @Transactional
    public int resetForSuccessfulCycle(User user, BillingCycle billingCycle) {
        CreditAccount account = getOrCreateCreditAccount(user);
        int previousBalance = account.getCreditsRemaining();
        if (previousBalance < 0) {
            throw new IllegalStateException("Credit balance cannot be negative");
        }

        if (previousBalance > 0) {
            creditTransactionRepository.save(CreditTransaction.forCycle(
                    user,
                    billingCycle,
                    CreditTransactionType.CYCLE_EXPIRY,
                    -previousBalance
            ));
        }
        creditTransactionRepository.save(CreditTransaction.forCycle(
                user,
                billingCycle,
                CreditTransactionType.CYCLE_GRANT,
                MONTHLY_CREDIT_BALANCE
        ));

        account.resetTo(MONTHLY_CREDIT_BALANCE);
        creditAccountRepository.save(account);
        return account.getCreditsRemaining();
    }

    @Transactional
    public int deductForRedemption(
            User user,
            Long redemptionId,
            int creditsSpent
    ) {
        CreditAccount account = creditAccountRepository
                .findByUserIdForUpdate(user.getId())
                .orElseGet(() -> creditAccountRepository.save(
                        CreditAccount.create(user)
                ));
        account.deduct(creditsSpent);
        creditTransactionRepository.save(CreditTransaction.forRedemption(
                user,
                redemptionId,
                creditsSpent
        ));
        creditAccountRepository.save(account);
        return account.getCreditsRemaining();
    }

    @Transactional
    public List<CreditTransactionResponse> getTransactions(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
        getOrCreateCreditAccount(user);
        return creditTransactionRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId)
                .stream()
                .map(transaction -> new CreditTransactionResponse(
                        transaction.getType(),
                        transaction.getAmount(),
                        transaction.getCreatedAt()
                ))
                .toList();
    }
}
