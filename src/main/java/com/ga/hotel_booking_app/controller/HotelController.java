package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.CreateHotelRequest;
import com.ga.hotel_booking_app.dto.request.UpdateHotelRequest;
import com.ga.hotel_booking_app.dto.request.UpdateHotelStatusRequest;
import com.ga.hotel_booking_app.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/hotels")
public class HotelController {
    @Autowired
    private HotelService hotelService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_MANAGER')")
    @Operation(summary = "Create a new hotel", description = "Create a new hotel account (with pending status  for admin approval if created by manager)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Hotel created successfully"),
            @ApiResponse(responseCode = "409", description = "Hotel already exists"),
            @ApiResponse(responseCode = "403", description = "Only admin and hotel manager can create hotel"),
            @ApiResponse(responseCode = "400", description = "Manager id is required"),
    })
    public ResponseEntity<?> createHotel(@Valid @RequestBody CreateHotelRequest request) {
        System.out.println("Hotel Controller calling ---> create");
        return hotelService.createHotel(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hotel information by Id", description = "Fetches all hotel information by Id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List all hotel details"),
            @ApiResponse(responseCode = "404", description = "Hotel not found")
    })
    public ResponseEntity<?> getHotel(@PathVariable("id") Long id) {
        System.out.println("Hotel Controller calling ---> get hotel");
        return hotelService.getHotel(id);
    }
    @GetMapping
    @Operation(summary = "Get all active hotels", description = "Fetches all hotels with its information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List all hotels with its details"),
    })
    public ResponseEntity<?> getHotels(@RequestParam(required = false) String search,
                                       @RequestParam(required = false) String name,
                                       @RequestParam(required = false) String country,
                                       @RequestParam(required = false) String city,
                                       Pageable pageable) {

        System.out.println("Hotel Controller calling ---> get hotels");
        return hotelService.getHotels(search, name, country, city, pageable);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all pending hotels ", description = "Fetches all hotels that is need approval")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List all hotels with pending status"),
            @ApiResponse(responseCode = "403", description = "Only admin can view pending hotels")
    })
    public ResponseEntity<?> getPendingHotels(Pageable pageable) {
        System.out.println("Hotel Controller calling ---> get pending hotels");
        return hotelService.getPendingHotels(pageable);
    }
    @GetMapping("/my-hotels")
    @PreAuthorize("hasRole('HOTEL_MANAGER')")
    @Operation(summary = "Get all manager hotels ", description = "Fetches all hotels that are assigned to this manager")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List all hotels that is assigned to logged in manager"),
            @ApiResponse(responseCode = "403", description = "Only manager can view their own hotels")
    })
    public ResponseEntity<?> getMangerHotels(Pageable pageable) {
        System.out.println("Hotel Controller calling ---> get manager hotels");
        return hotelService.getMangerHotels(pageable);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_MANAGER')")
    @Operation(summary = "Update hotel information", description = "Update hotel information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Update hotel details"),
            @ApiResponse(responseCode = "404", description = "Hotel or manager are not found"),
            @ApiResponse(responseCode = "400", description = "Selected user is not a manager"),
            @ApiResponse(responseCode = "403", description = "Only admin or manager can update their hotel information"),
    })
    public ResponseEntity<?> updateHotel(@PathVariable("id") Long id,
                                         @Valid @RequestBody UpdateHotelRequest request) {
        System.out.println("Hotel Controller calling ---> update hotel information");
        return hotelService.updateHotel(id, request);
    }
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update hotel status", description = "Update hotel status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Update hotel status"),
            @ApiResponse(responseCode = "404", description = "Hotel not found"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "403", description = "Only admin can update their hotel status"),

    })
    public ResponseEntity<?> updateHotelStatus(@PathVariable("id") Long id,
                                         @Valid @RequestBody UpdateHotelStatusRequest request) {
        System.out.println("Hotel Controller calling ---> update hotel status");
        return hotelService.updateHotelStatus(id, request);
    }
    @PostMapping("/{hotelId}/managers/{managerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign Manager to a hotel", description = "Assign manager to a specific hotel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assign a manager to a hotel"),
            @ApiResponse(responseCode = "404", description = "Hotel or manager are not found"),
            @ApiResponse(responseCode = "400", description = "User is not a hotel manager or is already assigned"),
            @ApiResponse(responseCode = "403", description = "Only admin can assign a manager to a hotel"),
    })
    public ResponseEntity<?> assignManager(@PathVariable("hotelId") Long hotelId,
            @PathVariable("managerId") Long managerId) {

        return hotelService.assignManager(hotelId, managerId);
    }

}
