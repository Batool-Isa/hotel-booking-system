package com.ga.hotel_booking_app.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class BookingResponse {
    private Long id;
    private String bookingReference;
    private Long hotelId;
    private String hotelName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String status;
    private int adults;
    private int children;
    private BigDecimal totalAmount;
    private String specialRequest;
    private List<BookingRoomResponse> rooms;
}