package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.AmenityRequest;
import com.ga.hotel_booking_app.dto.request.HotelAmenityRequest;
import com.ga.hotel_booking_app.service.AmenityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Amenities", description = "Amenity catalog and the amenities of each hotel")
public class AmenityController {

    @Autowired
    private AmenityService amenityService;

    @GetMapping("/api/amenities")
    @Operation(summary = "List all amenities", description = "The catalog every hotel can choose from Public")
    public ResponseEntity<?> getAmenities() {
        return amenityService.getAmenities();
    }

    @PostMapping("/api/amenities")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_MANAGER')")
    @Operation(summary = "Create an amenity", description = "Adds a new amenity to the catalog")
    public ResponseEntity<?> createAmenity(@Valid @RequestBody AmenityRequest request) {
        return amenityService.createAmenity(request);
    }

    @PutMapping("/api/amenities/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an amenity", description = "Renames or re-describes a catalog amenity")
    public ResponseEntity<?> updateAmenity(@PathVariable("id") Long id, @Valid @RequestBody AmenityRequest request) {
        return amenityService.updateAmenity(id, request);
    }

    @DeleteMapping("/api/amenities/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an amenity", description = "Only works if no hotel uses it")
    public ResponseEntity<?> deleteAmenity(@PathVariable("id") Long id) {
        return amenityService.deleteAmenity(id);
    }


    @PostMapping("/api/hotels/{hotelId}/amenities")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_MANAGER')")
    @Operation(summary = "Add an amenity to a hotel", description = "Links a catalog amenity to the hotel, with an optional description and extra cost")
    public ResponseEntity<?> addAmenityToHotel(@PathVariable("hotelId") Long hotelId,
                                               @Valid @RequestBody HotelAmenityRequest request) {
        return amenityService.addAmenityToHotel(hotelId, request);
    }

    @PutMapping("/api/hotels/{hotelId}/amenities/{amenityId}")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_MANAGER')")
    @Operation(summary = "Update a hotel amenity", description = "Changes the description and extra cost of an amenity of this hotel")
    public ResponseEntity<?> updateHotelAmenity(@PathVariable("hotelId") Long hotelId,
                                                @PathVariable("amenityId") Long amenityId,
                                                @Valid @RequestBody HotelAmenityRequest request) {
        return amenityService.updateHotelAmenity(hotelId, amenityId, request);
    }

    @DeleteMapping("/api/hotels/{hotelId}/amenities/{amenityId}")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_MANAGER')")
    @Operation(summary = "Remove an amenity from a hotel", description = "Unlinks the amenity from this hotel (it stays in the catalog)")
    public ResponseEntity<?> removeAmenityFromHotel(@PathVariable("hotelId") Long hotelId,
                                                    @PathVariable("amenityId") Long amenityId) {
        return amenityService.removeAmenityFromHotel(hotelId, amenityId);
    }
}
