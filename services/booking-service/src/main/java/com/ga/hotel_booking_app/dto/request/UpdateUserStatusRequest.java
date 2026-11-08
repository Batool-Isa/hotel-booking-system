package com.ga.hotel_booking_app.dto.request;

import com.ga.hotel_booking_app.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserStatusRequest {
    @NotNull(message = "Status is required")
    @Schema(description = "Account status", allowableValues = {"ACTIVE", "INACTIVE"})
    private User.Status status;
}
