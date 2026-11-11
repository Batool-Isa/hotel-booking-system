package com.ga.hotelservice.repository;

import com.ga.hotelservice.model.ChildPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface ChildPolicyRepository extends JpaRepository<ChildPolicy, Long> {
    Optional<ChildPolicy> findByHotelId(Long hotelId);
    boolean existsByHotelId(Long hotelId);
}