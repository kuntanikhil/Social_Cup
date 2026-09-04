package com.socialcup.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "homeNeighbourhood")
    @Query("select u from User u where u.id = :id")
    Optional<User> findProfileById(@Param("id") Long id);

    boolean existsByEmail(String email);
}
