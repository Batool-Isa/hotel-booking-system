package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.AvailabilityRequest;
import com.ga.hotel_booking_app.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    @Autowired
    private AvailabilityService availabilityService;

    @PostMapping("/search")
    public ResponseEntity<?> searchAvailability(
            @Valid @RequestBody AvailabilityRequest request) {
        System.out.println("Availability Controller calling --> search availability");
        return availabilityService.searchAvailability(request);
    }
}
