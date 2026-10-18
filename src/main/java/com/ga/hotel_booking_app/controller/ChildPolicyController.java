package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.CreateChildPolicyRequest;
import com.ga.hotel_booking_app.service.ChildPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "Create Child Policy", description = "Create a new child policy for a hotel")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Child Policy created"),
            @ApiResponse(responseCode = "404", description = "Hotel not found"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Manager can only create policy for their own hotels"),
            @ApiResponse(responseCode = "409", description = "Hotel already have a child policy"),
            @ApiResponse(responseCode = "400", description = "Invalid Ages (Infant maximum age must be less than child minimum age)"),
    })
    public ResponseEntity<?> createChildPolicy(@PathVariable("hotelId") Long hotelId,
                                               @Valid @RequestBody CreateChildPolicyRequest request) {
        return childPolicyService.createChildPolicy(hotelId, request);
    }

    @GetMapping
    @Operation(summary = "Get Child Policy", description = "Fetch child policy of a hotel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Get child policy"),
            @ApiResponse(responseCode = "404", description = "Hotel or policy are not found"),
    })
    public ResponseEntity<?> getChildPolicy(@PathVariable("hotelId") Long hotelId) {
        return childPolicyService.getChildPolicy(hotelId);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_MANAGER')")
    @Operation(summary = "Update Child Policy", description = "Update a child policy for a hotel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Update child policy successfully"),
            @ApiResponse(responseCode = "404", description = "Hotel not found"),
            @ApiResponse(responseCode = "403", description = "Manager are forbidden to create a child policy for a hotel that is not assign to it"),
            @ApiResponse(responseCode = "400", description = "Invalid Ages (Infant maximum age must be less than child minimum age)"),
    })
    public ResponseEntity<?> updateChildPolicy(@PathVariable("hotelId") Long hotelId,
                                               @Valid @RequestBody CreateChildPolicyRequest request) {
        return childPolicyService.updateChildPolicy(hotelId, request);
    }
}