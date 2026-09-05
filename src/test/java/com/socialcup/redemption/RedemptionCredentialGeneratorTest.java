package com.socialcup.redemption;

import com.socialcup.security.SecureTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedemptionCredentialGeneratorTest {

    @Test
    void generatesHashedOneTimeCredentials() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        RedemptionCredentialGenerator generator =
                new RedemptionCredentialGenerator(
                        passwordEncoder,
                        new SecureTokenService()
                );

        RedemptionCredentialGenerator.GeneratedRedemptionCredentials credentials =
                generator.generate();

        assertTrue(credentials.qrToken().matches("[A-Za-z0-9_-]{43}"));
        assertTrue(credentials.backupCode().matches("\\d{6}"));
        assertEquals(64, credentials.qrTokenHash().length());
        assertFalse(credentials.qrTokenHash().contains(credentials.qrToken()));
        assertFalse(credentials.backupCodeHash().contains(credentials.backupCode()));
        assertTrue(passwordEncoder.matches(
                credentials.backupCode(),
                credentials.backupCodeHash()
        ));
    }
}
