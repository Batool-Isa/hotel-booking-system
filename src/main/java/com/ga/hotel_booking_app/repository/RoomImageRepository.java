package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {
    List<RoomImage> findByRoomId(Long roomId);
    long countByRoomId(Long roomId);
}
