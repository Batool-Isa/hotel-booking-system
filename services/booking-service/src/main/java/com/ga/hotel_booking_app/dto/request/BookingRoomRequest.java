package com.ga.hotel_booking_app.dto.request;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BookingRoomRequest {

    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotEmpty(message = "At least one guest is required")
    private List<BookingGuestRequest> guests;
}