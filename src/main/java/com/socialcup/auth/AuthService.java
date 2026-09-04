package com.socialcup.auth;

import com.socialcup.profile.ProfileResponse;
import com.socialcup.security.JwtService;
import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AuthService {

    private static final Duration REFRESH_TOKEN_LIFETIME = Duration.ofDays(30);
    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;
    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private static final String INVALID_REFRESH_TOKEN = "Invalid or expired refresh token";

    private final UserRepository userRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom secureRandom;
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            AuthIdentityRepository authIdentityRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.authIdentityRepository = authIdentityRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.secureRandom = new SecureRandom();
        this.dummyPasswordHash = passwordEncoder.encode("social-cup-dummy-password");
    }

    @Transactional
    public ProfileResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String displayName = request.displayName().trim();
        validatePasswordLength(request.password());

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        try {
            User user = userRepository.saveAndFlush(User.create(email, displayName));
            String passwordHash = passwordEncoder.encode(request.password());
            authIdentityRepository.save(AuthIdentity.email(user, passwordHash));
            return ProfileResponse.from(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered",
                    exception
            );
        }
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        validatePasswordLength(request.password());

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw invalidCredentials();
        }

        AuthIdentity identity = authIdentityRepository
                .findByUserAndProvider(user, AuthProvider.EMAIL)
                .orElse(null);
        if (identity == null) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw invalidCredentials();
        }

        if (!passwordEncoder.matches(request.password(), identity.getPasswordHash())
                || !"ACTIVE".equals(user.getAccountStatus())) {
            throw invalidCredentials();
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHash(hashRefreshToken(request.refreshToken()))
                .orElseThrow(this::invalidRefreshToken);

        if (!storedToken.isUsableAt(now)
                || !"ACTIVE".equals(storedToken.getUser().getAccountStatus())) {
            throw invalidRefreshToken();
        }

        storedToken.revoke(now);
        return issueTokens(storedToken.getUser());
    }

    @Transactional
    public void logout(Long authenticatedUserId, RefreshTokenRequest request) {
        refreshTokenRepository.findByTokenHash(hashRefreshToken(request.refreshToken()))
                .filter(token -> token.getUser().getId().equals(authenticatedUserId))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.revoke(OffsetDateTime.now(ZoneOffset.UTC)));
    }

    private AuthResponse issueTokens(User user) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String rawRefreshToken = generateRefreshToken();

        refreshTokenRepository.save(new RefreshToken(
                user,
                hashRefreshToken(rawRefreshToken),
                now.plus(REFRESH_TOKEN_LIFETIME),
                now
        ));

        return new AuthResponse(
                jwtService.createAccessToken(user.getId()),
                rawRefreshToken,
                "Bearer",
                jwtService.getAccessTokenLifetimeSeconds()
        );
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashRefreshToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must not exceed 72 UTF-8 bytes"
            );
        }
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
    }

    private ResponseStatusException invalidRefreshToken() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_REFRESH_TOKEN);
    }
}
