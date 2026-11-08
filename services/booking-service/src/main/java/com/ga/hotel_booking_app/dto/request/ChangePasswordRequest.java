package com.ga.hotel_booking_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ChangePasswordRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be more than 8 character")
    private String newPassword;

    @NotBlank(message = "Confirmed password is required")
    private String confirmedPassword;
}
