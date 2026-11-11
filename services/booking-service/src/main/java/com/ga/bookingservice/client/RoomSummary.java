package com.ga.bookingservice.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoomSummary {
    private Long id;
    private Long hotelId;
    private String roomNumber;
    private String roomTypeName;
    private BigDecimal pricePerNight;
    private Integer maxAdults;
    private Integer maxChildren;
    private Integer maxOccupancy;
    private String status;
}
