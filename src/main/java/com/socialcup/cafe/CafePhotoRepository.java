package com.socialcup.cafe;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CafePhotoRepository extends JpaRepository<CafePhoto, Long> {

    List<CafePhoto> findByCafeIdOrderByDisplayOrderAscIdAsc(Long cafeId);
}
