package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.ChildPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface ChildPolicyRepository extends JpaRepository<ChildPolicy, Long> {
    Optional<ChildPolicy> findByHotelId(Long hotelId);
    boolean existsByHotelId(Long hotelId);
}