package com.ga.bookingservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HotelSummary {
    private Long id;
    private String name;
    private String city;
    private String country;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String status;
}
