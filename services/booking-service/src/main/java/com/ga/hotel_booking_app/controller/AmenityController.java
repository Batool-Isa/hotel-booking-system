package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.AmenityRequest;
import com.ga.hotel_booking_app.dto.request.HotelAmenityRequest;
import com.ga.hotel_booking_app.service.AmenityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "List all amenities", description = "Shows every amenity in the catalog that hotels can choose from. No login needed")
    @ApiResponse(responseCode = "200", description = "List of all amenities")
    public ResponseEntity<?> getAmenities() {
        return amenityService.getAmenities();
    }

    @PostMapping("/api/amenities")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_MANAGER')")
    @Operation(summary = "Create an amenity", description = "Adds a new amenity (like WiFi or Pool) to the catalog so hotels can use it")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Amenity created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid amenity information"),
            @ApiResponse(responseCode = "403", description = "Only admin or hotel manager can create an amenity"),
            @ApiResponse(responseCode = "409", description = "Amenity with the same name already exists")
    })
    public ResponseEntity<?> createAmenity(@Valid @RequestBody AmenityRequest request) {
        return amenityService.createAmenity(request);
    }

    @PutMapping("/api/amenities/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an amenity", description = "Changes the name or description of an amenity in the catalog")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Amenity updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid amenity information"),
            @ApiResponse(responseCode = "403", description = "Only admin can update an amenity"),
            @ApiResponse(responseCode = "404", description = "Amenity not found"),
            @ApiResponse(responseCode = "409", description = "Another amenity already has this name")
    })
    public ResponseEntity<?> updateAmenity(@PathVariable("id") Long id, @Valid @RequestBody AmenityRequest request) {
        return amenityService.updateAmenity(id, request);
    }

    @DeleteMapping("/api/amenities/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an amenity", description = "Removes an amenity from the catalog. You can only delete it if no hotel is using it")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Amenity deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Amenity is still used by at least one hotel"),
            @ApiResponse(responseCode = "403", description = "Only admin can delete an amenity"),
            @ApiResponse(responseCode = "404", description = "Amenity not found")
    })
    public ResponseEntity<?> deleteAmenity(@PathVariable("id") Long id) {
        return amenityService.deleteAmenity(id);
    }


    @PostMapping("/api/hotels/{hotelId}/amenities")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_MANAGER')")
    @Operation(summary = "Add an amenity to a hotel", description = "Adds an amenity from the catalog to this hotel. You can also add a description and an extra cost")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Amenity added to the hotel successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid amenity information"),
            @ApiResponse(responseCode = "403", description = "Manager can only change amenities of their own hotels"),
            @ApiResponse(responseCode = "404", description = "Hotel or amenity not found"),
            @ApiResponse(responseCode = "409", description = "Hotel already has this amenity")
    })
    public ResponseEntity<?> addAmenityToHotel(@PathVariable("hotelId") Long hotelId,
                                               @Valid @RequestBody HotelAmenityRequest request) {
        return amenityService.addAmenityToHotel(hotelId, request);
    }

    @PutMapping("/api/hotels/{hotelId}/amenities/{amenityId}")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_MANAGER')")
    @Operation(summary = "Update a hotel amenity", description = "Changes the description or extra cost of an amenity in this hotel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hotel amenity updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid amenity information"),
            @ApiResponse(responseCode = "403", description = "Manager can only change amenities of their own hotels"),
            @ApiResponse(responseCode = "404", description = "Hotel not found or hotel does not have this amenity")
    })
    public ResponseEntity<?> updateHotelAmenity(@PathVariable("hotelId") Long hotelId,
                                                @PathVariable("amenityId") Long amenityId,
                                                @Valid @RequestBody HotelAmenityRequest request) {
        return amenityService.updateHotelAmenity(hotelId, amenityId, request);
    }

    @DeleteMapping("/api/hotels/{hotelId}/amenities/{amenityId}")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_MANAGER')")
    @Operation(summary = "Remove an amenity from a hotel", description = "Removes the amenity from this hotel only. It stays in the catalog for other hotels")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Amenity removed from the hotel"),
            @ApiResponse(responseCode = "403", description = "Manager can only change amenities of their own hotels"),
            @ApiResponse(responseCode = "404", description = "Hotel not found or hotel does not have this amenity")
    })
    public ResponseEntity<?> removeAmenityFromHotel(@PathVariable("hotelId") Long hotelId,
                                                    @PathVariable("amenityId") Long amenityId) {
        return amenityService.removeAmenityFromHotel(hotelId, amenityId);
    }
}
