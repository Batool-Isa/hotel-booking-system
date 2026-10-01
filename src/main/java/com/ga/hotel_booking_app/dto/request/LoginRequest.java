package com.ga.hotel_booking_app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide an email address")
    private String email;
    @NotBlank(message = "Password is required")
    private String password;
}
