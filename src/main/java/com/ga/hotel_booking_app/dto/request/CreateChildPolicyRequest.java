package com.ga.hotel_booking_app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CreateChildPolicyRequest {

    @NotNull(message = "Infant maximum age is required")
    @Min(value = 0, message = "Infant maximum age cannot be negative")
    @Max(value = 5, message = "Infant maximum age cannot exceed 5")
    private Integer infantMaxAge;

    @NotNull(message = "Child maximum age is required")
    @Min(value = 1, message = "Child maximum age must be at least 1")
    @Max(value = 17, message = "Child maximum age cannot exceed 17")
    private Integer childMaxAge;
}