package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByStatus(Room.Status status);
    Optional<Room> findByIdAndStatus(Long id, Room.Status status);
    List<Room> findByHotelId(Long hotelId);

}
