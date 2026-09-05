package com.socialcup.redemption;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Component
class RedemptionCredentialGenerator {

    private static final int QR_TOKEN_BYTES = 32;
    private static final int BACKUP_CODE_BOUND = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();
    private final PasswordEncoder passwordEncoder;

    RedemptionCredentialGenerator(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    GeneratedRedemptionCredentials generate() {
        byte[] tokenBytes = new byte[QR_TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);

        String qrToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
        String backupCode = String.format(
                Locale.ROOT,
                "%06d",
                secureRandom.nextInt(BACKUP_CODE_BOUND)
        );

        return new GeneratedRedemptionCredentials(
                qrToken,
                sha256(qrToken),
                backupCode,
                passwordEncoder.encode(backupCode)
        );
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
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
