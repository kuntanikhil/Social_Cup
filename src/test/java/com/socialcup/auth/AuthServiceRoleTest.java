package com.socialcup.auth;

import com.socialcup.profile.ProfileResponse;
import com.socialcup.security.JwtService;
import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import com.socialcup.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRoleTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthIdentityRepository authIdentityRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode("social-cup-dummy-password")).thenReturn("dummy-hash");
        authService = new AuthService(
                userRepository,
                authIdentityRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void publicRegistrationAlwaysCreatesMember() {
        when(userRepository.existsByEmail("new@socialcup.demo")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("secure-pass")).thenReturn("password-hash");

        ProfileResponse response = authService.register(new RegisterRequest(
                "NEW@socialcup.demo",
                "secure-pass",
                "New User"
        ));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertEquals(UserRole.MEMBER, userCaptor.getValue().getRole());
        assertEquals(UserRole.MEMBER, response.role());
    }

    @Test
    void publicRegistrationContractHasNoRoleInput() {
        boolean exposesRole = Arrays.stream(RegisterRequest.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("role"));

        assertFalse(exposesRole);
    }
}
