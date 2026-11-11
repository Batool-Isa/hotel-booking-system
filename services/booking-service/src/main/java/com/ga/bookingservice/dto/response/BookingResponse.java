package com.ga.bookingservice.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class BookingResponse {
    private Long id;
    private String bookingReference;
    private Long userId;
    private Long hotelId;
    private String hotelName;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private LocalDateTime createdAt;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String status;
    private LocalDateTime holdExpiresAt;
    private int adults;
    private int children;
    private BigDecimal totalAmount;
    private String specialRequest;
    private List<BookingRoomResponse> rooms;
    private boolean reviewed;
}