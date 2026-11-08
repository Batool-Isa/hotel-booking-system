package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.BookingRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRoomRepository extends JpaRepository<BookingRoom, Long> {
}