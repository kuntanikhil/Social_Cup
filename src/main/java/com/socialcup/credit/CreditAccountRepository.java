package com.socialcup.credit;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CreditAccountRepository extends JpaRepository<CreditAccount, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from CreditAccount account where account.userId = :userId")
    Optional<CreditAccount> findByUserIdForUpdate(@Param("userId") Long userId);
}
