package com.ga.bookingservice.controller;

import com.ga.bookingservice.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hotels/{hotelId}")
public class HotelReviewController {
    @Autowired
    private ReviewService reviewService;

    @GetMapping("/reviews")
    @Operation(summary = "Get hotel reviews and rating", description = "Fetch the reviews of a hotel and its average rating")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of reviews and average rating")
    })
    public ResponseEntity<?> getHotelReviews(@PathVariable("hotelId") Long hotelId) {
        return reviewService.getHotelReviews(hotelId);
    }

    @GetMapping("/rating")
    @Operation(summary = "Get hotel rating", description = "Fetch the average rating and the number of reviews of a hotel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Average rating and review count")
    })
    public ResponseEntity<?> getHotelRating(@PathVariable("hotelId") Long hotelId) {
        return reviewService.getHotelRating(hotelId);
    }
}
