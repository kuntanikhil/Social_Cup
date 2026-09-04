package com.socialcup.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CoffeePreferenceRepository extends JpaRepository<CoffeePreference, Long> {

    List<CoffeePreference> findByActiveTrueOrderByIdAsc();

    List<CoffeePreference> findByIdInAndActiveTrue(Collection<Long> ids);
}
