package com.ga.hotel_booking_app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BookingNotification {

    private String bookingReference;
    private String message;
    private String status;
}