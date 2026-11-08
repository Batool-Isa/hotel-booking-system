package com.ga.hotel_booking_app.repository;


import com.ga.hotel_booking_app.model.HotelAmenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HotelAmenityRepository extends JpaRepository<HotelAmenity, Long> {
    Optional<HotelAmenity> findByHotelIdAndAmenityId(Long hotelId, Long amenityId);
    boolean existsByAmenityId(Long amenityId);
}