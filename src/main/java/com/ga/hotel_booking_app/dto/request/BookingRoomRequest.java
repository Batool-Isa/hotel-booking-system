package com.ga.hotel_booking_app.dto.request;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class BookingRoomRequest {

    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotEmpty(message = "At least one guest is required")
    private List<BookingGuestRequest> guests;
}