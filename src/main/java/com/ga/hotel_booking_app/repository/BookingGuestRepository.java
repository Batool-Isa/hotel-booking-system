package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.BookingGuest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingGuestRepository extends JpaRepository<BookingGuest, Long> {
}