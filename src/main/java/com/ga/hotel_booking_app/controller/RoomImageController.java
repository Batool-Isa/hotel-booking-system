package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.service.RoomImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/hotels/{hotelId}/rooms/{roomId}/images")
@Tag(name = "Room images", description = "Upload, view and delete room photos")
public class RoomImageController {

    @Autowired
    private RoomImageService roomImageService;

    @GetMapping
    @Operation(summary = "Get room images", description = "Fetches all photos of one room")
    public ResponseEntity<?> getImages(@PathVariable("hotelId") Long hotelId, @PathVariable("roomId") Long roomId) {
        return roomImageService.getImages(hotelId, roomId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_MANAGER')")
    @Operation(summary = "Upload room images", description = "upload room image")
    public ResponseEntity<?> uploadImages(@PathVariable("hotelId") Long hotelId, @PathVariable("roomId") Long roomId,
                                          @RequestParam("images") List<MultipartFile> images) {
        return roomImageService.uploadImages(hotelId, roomId, images);
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_MANAGER')")
    @Operation(summary = "Delete a room image", description = "Removes one photo from a room")
    public ResponseEntity<?> deleteImage(@PathVariable("hotelId") Long hotelId, @PathVariable("roomId") Long roomId,
                                         @PathVariable("imageId") Long imageId) throws IOException {
        return roomImageService.deleteImage(hotelId, roomId, imageId);
    }
}
