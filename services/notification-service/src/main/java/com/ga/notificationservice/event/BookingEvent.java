package com.ga.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingEvent(
        String eventId,
        String type,
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
