package com.ga.hotel_booking_app.dto.reponse;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({
        "id", "name", "description"}
)
public class RoomTypeResponse {
    private Long id;
    private String name;
    private String description;
}