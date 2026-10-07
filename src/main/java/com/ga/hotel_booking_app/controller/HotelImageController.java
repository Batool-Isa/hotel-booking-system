package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.service.HotelImageService;
import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<?> uploadImages(@PathVariable("hotelId") Long id,
                                         @RequestParam("images")List<MultipartFile>  images,
                                         @RequestParam("altTexts") List<String> altTexts) {

        System.out.println("Hotel Image Controller calling ---> get hotel images");
        return hotelImageService.uploadImages(id, images, altTexts);
    }

    @GetMapping
    @Operation(summary = "Get all hotel images", description = "Fetches all image that belongs to this hotel")
    public ResponseEntity<?> getImages(@PathVariable("hotelId") Long id) {
        System.out.println("Hotel Image Controller calling ---> get hotel images");
        return hotelImageService.getImages(id);
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_MANAGER')")
    public ResponseEntity<?> deleteImage(@PathVariable("hotelId") Long hotelId,
                                         @PathVariable("imageId") Long imageId) throws IOException {
        System.out.println("Hotel Image Controller calling ---> delete hotel image");
        return hotelImageService.deleteImage(hotelId, imageId);
    }
}