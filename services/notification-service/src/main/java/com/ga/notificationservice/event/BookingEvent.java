package com.ga.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingEvent(
        String eventId,
        String type,
        Long bookingId,
        String bookingReference,
        String status,
        Long userId,
        Long hotelId,
        String hotelName,
        String hotelAddress,
        String hotelCity,
        String hotelCountry,
        BigDecimal hotelLatitude,
        BigDecimal hotelLongitude,
        String guestName,
        String guestEmail,
        LocalDate checkIn,
        LocalDate checkOut,
        int adults,
        int children,
        String specialRequest,
        BigDecimal totalAmount,
        List<Room> rooms,
        LocalDateTime occurredAt
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Room(
            String roomNumber,
            String roomTypeName,
            BigDecimal pricePerNight,
            List<Guest> guests
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Guest(
            String name,
            Integer age,
            String guestType
    ) {
    }
}
