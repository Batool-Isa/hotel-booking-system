package com.ga.hotel_booking_app.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class
AvailableHotelResponse {
    private Long hotelId;
    private String hotelName;
    private String city;
    private String country;
    private String address;
    private String profileImage;
    private Double averageRating;
    private List<RoomResponse> rooms = new ArrayList<>();
}