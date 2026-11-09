package com.ga.hotelservice.repository;

import com.ga.hotelservice.model.HotelImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelImageRepository extends JpaRepository<HotelImage, Long> {
List<HotelImage> findByHotelId(Long id);
 boolean existsByImageURL(String imageUrl);
}
