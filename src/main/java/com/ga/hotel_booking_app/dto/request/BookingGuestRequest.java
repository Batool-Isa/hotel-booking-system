package com.ga.hotel_booking_app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class BookingGuestRequest {

    @NotBlank(message = "Guest name is required")
    private String name;

    @NotNull(message = "Guest age is required")
    @Min(value = 0, message = "Age cannot be negative")
    private Integer age;
}