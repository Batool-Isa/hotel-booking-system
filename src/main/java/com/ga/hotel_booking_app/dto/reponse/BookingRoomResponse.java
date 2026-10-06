package com.ga.hotel_booking_app.dto.reponse;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class BookingRoomResponse {

    private Long roomId;
    private String roomNumber;
    private String roomTypeName;

    private BigDecimal pricePerNight;

    private List<BookingGuestResponse> guests;
}