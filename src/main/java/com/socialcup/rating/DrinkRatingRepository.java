package com.socialcup.rating;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DrinkRatingRepository extends JpaRepository<DrinkRating, Long> {

    Optional<DrinkRating> findByUserIdAndDrinkId(Long userId, Long drinkId);

    @Query("""
            select r
            from DrinkRating r
            join fetch r.drink d
            join fetch d.cafe
            where r.user.id = :userId
            order by r.stars desc, r.createdAt desc
            """)
    List<DrinkRating> findDiaryByUserId(@Param("userId") Long userId);

    @Query("""
            select new com.socialcup.rating.RatingAggregate(avg(r.stars), count(r))
            from DrinkRating r
            where r.drink.id = :drinkId
            """)
    RatingAggregate findDrinkRatingAggregate(@Param("drinkId") Long drinkId);

    @Query("""
            select new com.socialcup.rating.CafeRatingAggregate(
                r.drink.cafe.id,
                avg(r.stars),
                count(r)
            )
            from DrinkRating r
            where r.drink.cafe.id in :cafeIds
            group by r.drink.cafe.id
            """)
    List<CafeRatingAggregate> findCafeRatingAggregates(
            @Param("cafeIds") Collection<Long> cafeIds
    );
}
