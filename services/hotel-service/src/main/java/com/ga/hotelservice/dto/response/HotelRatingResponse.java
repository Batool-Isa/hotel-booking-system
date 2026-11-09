package com.ga.hotelservice.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ga.hotelservice.dto.response.ReviewResponse;
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
