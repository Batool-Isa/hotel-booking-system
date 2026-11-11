package com.ga.bookingservice.dto.request;

import com.ga.bookingservice.model.Booking;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingStatusRequest {

    @NotNull(message = "Status is required")
    private Booking.Status status;
}