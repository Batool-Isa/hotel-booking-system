package com.ga.hotel_booking_app.dto.reponse;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@JsonPropertyOrder({
        "id",
        "roomNumber",
        "floorNumber",
        "roomTypeId",
        "roomTypeName",
        "maxAdults",
        "maxChildren",
        "maxOccupancy",
        "pricePerNight",
        "status",
        "hotelId"
})
public class RoomResponse {

    private Long id;
    private String roomNumber;
    private String floorNumber;
    private BigDecimal pricePerNight;

    private Integer maxAdults;
    private Integer maxChildren;
    private Integer maxOccupancy;

    private String status;
    private Long roomTypeId;
    private String roomTypeName;
    private Long hotelId;
}