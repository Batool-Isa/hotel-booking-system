package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {
    Optional<Amenity> findByName(String name);
    boolean existsByNameIgnoreCase(String name);

}
