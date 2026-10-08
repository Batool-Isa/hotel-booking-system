package com.ga.hotel_booking_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class AmenityRequest {
    @NotBlank(message = "Amenity name is required")
    @Size(max = 60, message = "Amenity name must not exceed 60 characters")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
