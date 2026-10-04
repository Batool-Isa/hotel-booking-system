package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.Hotel;
import com.ga.hotel_booking_app.model.HotelImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelImageRepository extends JpaRepository<HotelImage, Long> {
List<HotelImage> findByHotelId(Long id);

}
