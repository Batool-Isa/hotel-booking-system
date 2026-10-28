package com.ga.hotel_booking_app.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ga.hotel_booking_app.model.Review;
import lombok.Setter;

import java.util.List;

@Setter
@JsonPropertyOrder({"averageRating","reviews"})
public class HotelRatingResponse {
    private double averageRating;
    private List<Review> reviews;
}
