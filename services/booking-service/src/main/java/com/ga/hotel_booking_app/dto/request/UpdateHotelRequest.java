package com.ga.hotel_booking_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class UpdateHotelRequest {

    @NotBlank(message = "Hotel name is required")
    @Size(min = 2, max = 100, message = "Hotel name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Hotel description is required")
    @Size(min = 10, max = 500, message = "Hotel description must be between 10 and 500 characters")
    private String description;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 255, message = "Address must be between 5 and 255 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    private String city;

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 100, message = "Country must be between 2 and 100 characters")
    private String country;

    @NotBlank(message = "Phone is required")
    @Size(min = 8, max = 12, message = "Phone must be at least 8 characters")
    private String  phone;
    private BigDecimal latitude;

    private BigDecimal longitude;
    private Long managerId;

}
