package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    boolean existsByName(String name);

    List<RoomType> findByStatus(RoomType.Status status);

    Optional<RoomType> findByIdAndStatus(Long id, RoomType.Status status);

}
