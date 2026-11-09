package com.ga.hotelservice.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HotelImageResponse {
    private Long id;
    private String imageUrl;
    private String AltText;
}