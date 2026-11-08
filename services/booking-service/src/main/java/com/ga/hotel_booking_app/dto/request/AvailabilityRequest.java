package com.ga.hotel_booking_app.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;


import java.time.LocalDate;

@Getter
public class AvailabilityRequest {
    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in can't be in the past")
    private LocalDate checkIn;
    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out must be in the future")
    private LocalDate checkOut;
    @NotNull
    @Min(1)
    private Integer adults;
    @Min(0)
    private Integer children = 0;
    @Size(max = 100)
    private String city;
}