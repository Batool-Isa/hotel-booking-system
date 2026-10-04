package com.ga.hotel_booking_app.dto.reponse;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@JsonPropertyOrder({"id", "name",
        "description", "address", "city", "country", "phone", "latitude", "longitude", "images", "amenities", "averageRating", "reviewCount", "createdAt", "updatedAt"
})
public class HotelResponse {
    private Long id;
    private String name;
    private String description;
    private String address;
    private String city;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phone;
    private List<HotelImageResponse> images;
    private List<AmenityResponse> amenities;
    private Double averageRating;
    private Integer reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}