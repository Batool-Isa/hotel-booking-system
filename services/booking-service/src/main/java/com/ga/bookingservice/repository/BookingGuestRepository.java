package com.ga.bookingservice.repository;

import com.ga.bookingservice.model.BookingGuest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingGuestRepository extends JpaRepository<BookingGuest, Long> {
}