package com.socialcup.auth;

import com.socialcup.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthIdentityRepository extends JpaRepository<AuthIdentity, Long> {

    Optional<AuthIdentity> findByUserAndProvider(User user, AuthProvider provider);
}
