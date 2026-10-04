package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {
boolean existsByName(String name);
}
