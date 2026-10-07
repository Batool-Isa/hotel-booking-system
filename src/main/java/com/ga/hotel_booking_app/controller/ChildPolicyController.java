package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.CreateChildPolicyRequest;
import com.ga.hotel_booking_app.service.ChildPolicyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hotels/{hotelId}/child-policy")
public class ChildPolicyController {

    @Autowired
    private ChildPolicyService childPolicyService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_MANAGER')")
    public ResponseEntity<?> createChildPolicy(@PathVariable("hotelId") Long hotelId,
            @Valid @RequestBody CreateChildPolicyRequest request) {
        return childPolicyService.createChildPolicy(hotelId, request);
    }

    @GetMapping
    public ResponseEntity<?> getChildPolicy(@PathVariable("hotelId") Long hotelId) {
        return childPolicyService.getChildPolicy(hotelId);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_MANAGER')")
    public ResponseEntity<?> updateChildPolicy(@PathVariable("hotelId") Long hotelId,
                                               @Valid @RequestBody CreateChildPolicyRequest request) {

        return childPolicyService.updateChildPolicy(hotelId, request);
    }
}