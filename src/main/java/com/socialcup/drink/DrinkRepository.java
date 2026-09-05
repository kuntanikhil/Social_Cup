package com.socialcup.drink;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DrinkRepository extends JpaRepository<Drink, Long> {

    Optional<Drink> findByIdAndActiveTrue(Long id);

    List<Drink> findByCafeIdOrderByNameAsc(Long cafeId);

    List<Drink> findByCafeIdAndActiveTrueOrderByNameAsc(Long cafeId);

    List<Drink> findByActiveTrueAndSignatureTrueOrderByNameAsc();

    @Query("""
            select d
            from Drink d
            join fetch d.cafe c
            where d.active = true
              and c.active = true
              and c.id in :cafeIds
            """)
    List<Drink> findActiveDrinksForActiveCafes(@Param("cafeIds") Collection<Long> cafeIds);

    @Query("select min(d.creditPrice) from Drink d where d.cafe.id = :cafeId and d.active = true")
    Optional<Integer> findMinimumActiveCreditPriceByCafeId(@Param("cafeId") Long cafeId);
}
