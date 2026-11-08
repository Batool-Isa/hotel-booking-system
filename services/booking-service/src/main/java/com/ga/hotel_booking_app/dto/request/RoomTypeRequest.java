package com.ga.hotel_booking_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class RoomTypeRequest {

    @NotBlank(message = "Room Type name is required")
    @Size(min = 2, max = 100, message = "Room Type name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Room Type description is required")
    @Size(min = 10, max = 500, message = "Room Type description must be between 10 and 500 characters")
    private String description;
}
