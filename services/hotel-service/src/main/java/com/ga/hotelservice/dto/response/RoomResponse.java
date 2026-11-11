package com.ga.hotelservice.dto.response;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

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
    private String imageUrl;
    private List<String> imageUrls;
    private String roomTypeDescription;
}