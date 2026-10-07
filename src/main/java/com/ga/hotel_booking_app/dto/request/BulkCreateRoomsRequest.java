package com.ga.hotel_booking_app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class BulkCreateRoomsRequest {
    @NotEmpty(message = "Send at least one room")
    @Size(max = 100, message = "You can add at most 100 rooms at once")
    private List<@Valid CreateRoomRequest> rooms;
}
