package com.ga.hotel_booking_app.dto.request;

import com.ga.hotel_booking_app.model.Room;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateRoomStatusRequest {
    @NotNull(message = "Status is required")
    private Room.Status status;
}
