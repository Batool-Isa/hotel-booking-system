package com.ga.hotelservice.dto.request;

import com.ga.hotelservice.model.Room;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateRoomStatusRequest {
    @NotNull(message = "Status is required")
    private Room.Status status;
}
