package com.ga.hotel_booking_app.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class AvailabilityRequest {
    @NotNull
    @Future
    private LocalDate checkIn;
    @NotNull
    @Future
    private LocalDate checkOut;
    @NotNull
    @Min(1)
    private Integer adults;
    @Min(0)
    private Integer children = 0;
    @Size(max = 100)
    private String city;
}