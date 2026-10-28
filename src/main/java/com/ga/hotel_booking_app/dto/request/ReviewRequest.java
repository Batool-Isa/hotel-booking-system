package com.ga.hotel_booking_app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ReviewRequest {
    @NotNull
    @Min(1)
    @Max(5)
    private double rating;

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
}
