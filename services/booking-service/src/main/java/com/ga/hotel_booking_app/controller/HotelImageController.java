package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.service.HotelImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/hotels/{hotelId}/images")
public class HotelImageController {
    @Autowired
    private HotelImageService hotelImageService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_MANAGER')")
    @Operation(summary = "Upload hotel images", description = "Upload all hotel images")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image uploaded successfully"),
            @ApiResponse(responseCode = "404", description = "Hotel not found"),
            @ApiResponse(responseCode = "400", description = "Invalid upload request"),
            @ApiResponse(responseCode = "403", description = "Only admin & assigned manager can upload hotel images"),

    })
    public ResponseEntity<?> uploadImages(@PathVariable("hotelId") Long id,
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam("altTexts") List<String> altTexts) {
        System.out.println("Hotel Image Controller calling ---> upload hotel images");
        return hotelImageService.uploadImages(id, images, altTexts);
    }

    @GetMapping
    @Operation(summary = "Get all hotel images", description = "Fetches all image that belongs to this hotel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List all hotel's images"),
    })
    public ResponseEntity<?> getImages(@PathVariable("hotelId") Long id) {
        System.out.println("Hotel Image Controller calling ---> get hotel images");
        return hotelImageService.getImages(id);
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_MANAGER')")
    @Operation(summary = "Delete a hotel image", description = "Deletes a hotel image by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "delete image successfully"),
            @ApiResponse(responseCode = "404", description = "Hotel or image are not found"),
            @ApiResponse(responseCode = "403", description = "Only admin & assigned manager can delete hotel images"),
    })
    public ResponseEntity<?> deleteImage(@PathVariable("hotelId") Long hotelId,
            @PathVariable("imageId") Long imageId) throws IOException {
        System.out.println("Hotel Image Controller calling ---> delete hotel image");
        return hotelImageService.deleteImage(hotelId, imageId);
    }
}