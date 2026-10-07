package com.ga.hotel_booking_app.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class HotelAmenityRequest {
    @NotNull(message = "Amenity is required")
    private Long amenityId;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @DecimalMin(value = "0.00", message = "Additional cost can't be negative")
    private BigDecimal additionalCost;
}
