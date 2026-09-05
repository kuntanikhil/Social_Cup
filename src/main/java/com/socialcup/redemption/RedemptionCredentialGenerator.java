package com.socialcup.redemption;

import com.socialcup.security.SecureTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

@Component
class RedemptionCredentialGenerator {

    private static final int BACKUP_CODE_BOUND = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();
    private final PasswordEncoder passwordEncoder;
    private final SecureTokenService secureTokenService;

    RedemptionCredentialGenerator(
            PasswordEncoder passwordEncoder,
            SecureTokenService secureTokenService
    ) {
        this.passwordEncoder = passwordEncoder;
        this.secureTokenService = secureTokenService;
    }

    GeneratedRedemptionCredentials generate() {
        String qrToken = secureTokenService.generateToken();
        String backupCode = String.format(
                Locale.ROOT,
                "%06d",
                secureRandom.nextInt(BACKUP_CODE_BOUND)
        );

        return new GeneratedRedemptionCredentials(
                qrToken,
                secureTokenService.hash(qrToken),
                backupCode,
                passwordEncoder.encode(backupCode)
        );
    }

    static final class GeneratedRedemptionCredentials {

        private final String qrToken;
        private final String qrTokenHash;
        private final String backupCode;
        private final String backupCodeHash;

        GeneratedRedemptionCredentials(
                String qrToken,
                String qrTokenHash,
                String backupCode,
                String backupCodeHash
        ) {
            this.qrToken = qrToken;
            this.qrTokenHash = qrTokenHash;
            this.backupCode = backupCode;
            this.backupCodeHash = backupCodeHash;
        }

        String qrToken() {
            return qrToken;
        }

        String qrTokenHash() {
            return qrTokenHash;
        }

        String backupCode() {
            return backupCode;
        }

        String backupCodeHash() {
            return backupCodeHash;
        }
    }
}
