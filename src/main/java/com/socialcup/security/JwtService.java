package com.socialcup.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final String ISSUER = "social-cup-api";
    private static final Duration ACCESS_TOKEN_LIFETIME = Duration.ofMinutes(15);

    private final SecretKey signingKey;

    public JwtService(@Value("${JWT_SECRET}") String encodedSecret) {
        this.signingKey = createSigningKey(encodedSecret);
    }

    public String createAccessToken(Long userId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(ACCESS_TOKEN_LIFETIME);

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .claim("userId", userId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.valueOf(claims.getSubject());
    }

    public long getAccessTokenLifetimeSeconds() {
        return ACCESS_TOKEN_LIFETIME.toSeconds();
    }

    private SecretKey createSigningKey(String encodedSecret) {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be set");
        }

        try {
            byte[] keyBytes = Decoders.BASE64.decode(encodedSecret.trim());
            if (keyBytes.length < 32) {
                throw new IllegalStateException(
                        "JWT_SECRET must be a Base64-encoded key of at least 32 bytes"
                );
            }
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "JWT_SECRET must be a valid Base64-encoded key of at least 32 bytes",
                    exception
            );
        }
    }
}
