package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.CreateHotelRequest;
import com.ga.hotel_booking_app.dto.request.UpdateHotelRequest;
import com.ga.hotel_booking_app.dto.request.UpdateHotelStatusRequest;
import com.ga.hotel_booking_app.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Create a new hotel", description = "Create a new hotel account and send a verification email")
    public ResponseEntity<?> createHotel(@Valid @RequestBody CreateHotelRequest request) {
        System.out.println("Hotel Controller calling ---> create");
        return hotelService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hotel information by Id", description = "Fetches all hotel information by Id")
    public ResponseEntity<?> getHotel(@PathVariable("id") Long id) {
        System.out.println("Hotel Controller calling ---> get hotel");
        return hotelService.getHotel(id);
    }
    @GetMapping
    @Operation(summary = "Get all active hotels", description = "Fetches all hotels with its information")
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
    public ResponseEntity<?> getPendingHotels(Pageable pageable) {
        System.out.println("Hotel Controller calling ---> get pending hotels");
        return hotelService.getPendingHotels(pageable);
    }
    @GetMapping("/my-hotels")
    @PreAuthorize("hasRole('HOTEL_MANAGER')")
    @Operation(summary = "Get all manager hotels ", description = "Fetches all hotels tha tis assigned to this manager")
    public ResponseEntity<?> getMangerHotels(Pageable pageable) {
        System.out.println("Hotel Controller calling ---> get manager hotels");
        return hotelService.getMangerHotels(pageable);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_MANAGER')")
    @Operation(summary = "Update hotel information", description = "Update hotel information")
    public ResponseEntity<?> updateHotel(@PathVariable("id") Long id,
                                         @Valid @RequestBody UpdateHotelRequest request) {
        System.out.println("Hotel Controller calling ---> update hotel information");
        return hotelService.updateHotel(id, request);
    }
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update hotel status", description = "Update hotel status")
    public ResponseEntity<?> updateHotelStatus(@PathVariable("id") Long id,
                                         @Valid @RequestBody UpdateHotelStatusRequest request) {
        System.out.println("Hotel Controller calling ---> update hotel status");
        return hotelService.updateHotelStatus(id, request);
    }
    @PostMapping("/{hotelId}/managers/{managerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign Manager to a hotel", description = "Assign manager ro a specific hotel")
    public ResponseEntity<?> assignManager(@PathVariable("hotelId") Long hotelId,
            @PathVariable("managerId") Long managerId) {

        return hotelService.assignManager(hotelId, managerId);
    }

}
