package com.ga.hotel_booking_app.dto.request;

import com.ga.hotel_booking_app.model.Booking;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class BookingStatusRequest {

    @NotNull(message = "Status is required")
    private Booking.Status status;
}