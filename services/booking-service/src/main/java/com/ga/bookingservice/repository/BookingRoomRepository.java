package com.ga.bookingservice.repository;

import com.ga.bookingservice.model.BookingRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRoomRepository extends JpaRepository<BookingRoom, Long> {
}