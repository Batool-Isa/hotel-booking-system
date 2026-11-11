package com.ga.bookingservice.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AvailableRoom {
    private Long id;
    private Long hotelId;
    private String roomNumber;
    private String floorNumber;
    private Long roomTypeId;
    private String roomTypeName;
    private String roomTypeDescription;
    private Integer maxAdults;
    private Integer maxChildren;
    private Integer maxOccupancy;
    private BigDecimal pricePerNight;
    private String status;
    private String imageUrl;
    private List<String> imageUrls;
}
