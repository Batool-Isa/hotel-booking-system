package com.ga.hotel_booking_app.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonPropertyOrder({"averageRating","reviews"})
public class HotelRatingResponse {
    private double averageRating;
    private List<ReviewResponse> reviews;
}
