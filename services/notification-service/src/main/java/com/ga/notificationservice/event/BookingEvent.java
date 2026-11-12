package com.ga.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// notification-service's own copy of the Kafka message. field names must match booking-service's
// BookingEvent (the JSON is the contract, not a shared Java class).
// ignoreUnknown: if booking-service adds a field later, this side ignores it instead of failing
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingEvent(
        String eventId,
        String type,                // BOOKING_CREATED, BOOKING_CONFIRMED, BOOKING_CANCELLED, BOOKING_EXPIRED, BOOKING_COMPLETED
        Long bookingId,
        String bookingReference,
        Long userId,
        Long hotelId,
        String hotelName,
        String guestName,
        String guestEmail,
        LocalDate checkIn,
        LocalDate checkOut,
        BigDecimal totalAmount,
        LocalDateTime occurredAt
) {
}
