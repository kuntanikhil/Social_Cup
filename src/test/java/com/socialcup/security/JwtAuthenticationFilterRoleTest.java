package com.socialcup.security;

import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import com.socialcup.user.UserRole;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterRoleTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FilterChain filterChain;
    @Mock
    private User user;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsMemberAuthorityFromDatabaseRole() throws Exception {
        authenticateAs(UserRole.MEMBER);

        assertTrue(SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_MEMBER")));
    }

    @Test
    void createsAdminAuthorityFromDatabaseRole() throws Exception {
        authenticateAs(UserRole.ADMIN);

        assertTrue(SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
        AuthenticatedUser principal = (AuthenticatedUser) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        assertEquals(UserRole.ADMIN, principal.role());
    }

    private void authenticateAs(UserRole role) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.parseUserId("access-token")).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(user.getAccountStatus()).thenReturn("ACTIVE");
        when(user.getId()).thenReturn(7L);
        when(user.getEmail()).thenReturn("user@socialcup.demo");
        when(user.getRole()).thenReturn(role);

        new JwtAuthenticationFilter(jwtService, userRepository)
                .doFilterInternal(request, response, filterChain);
    }
}
