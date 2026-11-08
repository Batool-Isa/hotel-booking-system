package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.AvailabilityRequest;
import com.ga.hotel_booking_app.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    @Autowired
    private AvailabilityService availabilityService;

    @PostMapping("/search")
    @Operation(summary = "Check hotel & room availability",
            description = "Find the available room and hotel for the given dates and number of gustes")
   @ApiResponses({
           @ApiResponse(responseCode = "200", description = "List of available hotel and room"),
           @ApiResponse(responseCode = "400", description = "Invalid search options like invalid dates etc.")
   })
    public ResponseEntity<?> searchAvailability(
            @Valid @RequestBody AvailabilityRequest request, Pageable pageable) {
        System.out.println("Availability Controller calling --> search availability");
        return availabilityService.searchAvailability(request, pageable);
    }
}
