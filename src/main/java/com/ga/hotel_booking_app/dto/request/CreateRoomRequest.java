package com.ga.hotel_booking_app.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class CreateRoomRequest {
    @NotNull(message = "Room type is required")
    private Long roomTypeId;
    @NotBlank(message = "Room number is required")
    @Size(max = 20, message = "Room number must not exceed 20 characters")
    private String roomNumber;
    @NotBlank(message = "Floor number is required")
    @Size(max = 10, message = "Floor number must not exceed 10 characters")
    private String floorNumber;
    @NotNull(message = "Price per night is required")
    @DecimalMin(value = "0.01", message = "Price per night must be greater than 0")
    private BigDecimal pricePerNight;
    @NotNull(message = "Max Adult number is required")
    @Min(value = 1, message = "Adult number  must be at least 1")
    private Integer maxAdults;
    @NotNull(message = "Max Children number is required")
    @Min(value = 1, message = "Children number  must be at least 1")
    private Integer maxChildren;
    @NotNull(message = "Max Occupancy number is required")
    @Min(value = 1, message = "Occupancy number  must be at least 1")
    private Integer maxOccupancy;
}