package com.socialcup.admin;

import com.socialcup.user.User;
import com.socialcup.user.UserRepository;
import com.socialcup.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class InitialAdminBootstrapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            InitialAdminBootstrapService.class
    );

    private final UserRepository userRepository;

    public InitialAdminBootstrapService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void configureInitialAdmin(String configuredEmail) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return;
        }
        if (configuredEmail == null || configuredEmail.isBlank()) {
            return;
        }

        String normalizedEmail = configuredEmail.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            LOGGER.warn("ADMIN_EMAIL configured but matching user does not exist");
            return;
        }

        user.promoteToAdmin();
        LOGGER.info("Initial admin role configured for {}", normalizedEmail);
    }
}
