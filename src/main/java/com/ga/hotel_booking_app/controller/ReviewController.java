package com.ga.hotel_booking_app.controller;


import com.ga.hotel_booking_app.dto.request.ReviewRequest;
import com.ga.hotel_booking_app.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/{bookingId}/review")
public class ReviewController {
    @Autowired
    private ReviewService reviewService;
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Post a view", description = "Add a review on a completed booking")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review added successfully"),
            @ApiResponse(responseCode = "400", description = "Booking not completed"),
            @ApiResponse(responseCode = "403", description = "This booking is not owned by you")
    })
    public ResponseEntity<?> postReview(@PathVariable("bookingId") Long id,
                                        @RequestBody ReviewRequest request){
        return reviewService.postReview(id, request);
    }
}
