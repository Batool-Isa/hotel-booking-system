package com.ga.hotel_booking_app.dto.reponse;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AmenityResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal additionalCost;
}