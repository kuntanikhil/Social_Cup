package com.socialcup.admin;

import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import com.socialcup.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialAdminBootstrapServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private User firstUser;
    @Mock
    private User secondUser;

    private InitialAdminBootstrapService service;

    @BeforeEach
    void setUp() {
        service = new InitialAdminBootstrapService(userRepository);
    }

    @Test
    void promotesMatchingExistingUserWhenNoAdminExists() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userRepository.findByEmail("admin@socialcup.demo"))
                .thenReturn(Optional.of(firstUser));

        service.configureInitialAdmin("  ADMIN@socialcup.demo ");

        verify(firstUser).promoteToAdmin();
    }

    @Test
    void doesNotPromoteAnotherUserOnceAnAdminExists() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

        service.configureInitialAdmin("second@socialcup.demo");

        verify(userRepository, never()).findByEmail("second@socialcup.demo");
        verify(secondUser, never()).promoteToAdmin();
    }

    @Test
    void missingConfigurationDoesNothing() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);

        service.configureInitialAdmin("  ");

        verify(userRepository, never()).findByEmail(org.mockito.ArgumentMatchers.anyString());
        verify(firstUser, never()).promoteToAdmin();
    }

    @Test
    void unknownEmailDoesNotCreateOrPromoteAUser() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userRepository.findByEmail("missing@socialcup.demo"))
                .thenReturn(Optional.empty());

        service.configureInitialAdmin("missing@socialcup.demo");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
        verify(firstUser, never()).promoteToAdmin();
    }
}
