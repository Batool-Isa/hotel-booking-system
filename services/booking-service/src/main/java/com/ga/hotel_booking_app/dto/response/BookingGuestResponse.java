package com.ga.hotel_booking_app.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingGuestResponse {

    private Long id;
    private String name;
    private Integer age;
    private String guestType;
}