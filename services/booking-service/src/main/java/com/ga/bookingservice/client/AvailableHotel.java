package com.ga.bookingservice.client;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AvailableHotel {
    private Long hotelId;
    private String hotelName;
    private String city;
    private String country;
    private String address;
    private String profileImage;
    private Double averageRating;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private List<AvailableRoom> rooms = new ArrayList<>();
}
