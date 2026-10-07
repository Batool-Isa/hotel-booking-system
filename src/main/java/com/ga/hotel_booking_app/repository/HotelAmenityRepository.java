package com.ga.hotel_booking_app.repository;


import com.ga.hotel_booking_app.model.HotelAmenity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelAmenityRepository extends JpaRepository<HotelAmenity, Long> {
}