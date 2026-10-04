package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {
boolean existsByName(String name);
    List<Hotel> findByStatus(Hotel.Status status);
    List<Hotel> findByManagers_Id(Long managerId);
    List<Hotel> findByNameContainingIgnoreCaseAndStatus(String name, Hotel.Status status);

}
