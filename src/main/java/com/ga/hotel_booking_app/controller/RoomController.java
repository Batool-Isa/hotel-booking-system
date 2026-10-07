package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.BulkCreateRoomsRequest;
import com.ga.hotel_booking_app.dto.request.CreateRoomRequest;
import com.ga.hotel_booking_app.dto.request.UpdateRoomStatusRequest;
import com.ga.hotel_booking_app.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/hotels/{hotelId}/rooms")
public class RoomController {
    
    @Autowired
    private RoomService roomService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Create new room", description = "Create a new room in a hotel")
    public ResponseEntity<?> createRoom(@PathVariable("hotelId") Long id, @Valid @RequestBody CreateRoomRequest request) {
        System.out.println("Room controller calling ---> create room");
        return roomService.createRoom(id, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Get hotels room", description = "Fetches all hotel room")
    public ResponseEntity<?> getHotelRooms(@PathVariable("hotelId") Long id, Pageable pageable) {
        System.out.println("Room controller calling ---> get hotel rooms");
        return roomService.getHotelRooms(id, pageable);
    }
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Create many rooms", description = "Adds up to 100 rooms in one request. If one is invalid, none is saved")
    public ResponseEntity<?> createRoomsBulk(@PathVariable("hotelId") Long id, @Valid @RequestBody BulkCreateRoomsRequest request) {
        return roomService.createRoomsBulk(id, request);
    }


    @GetMapping("/{roomId}")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Get one room", description = "Fetches the details of one room of this hotel")
    public ResponseEntity<?> getRoom(@PathVariable("hotelId") Long hotelId, @PathVariable("roomId") Long roomId) {
        return roomService.getRoom(hotelId, roomId);
    }

    @PutMapping("/{roomId}")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Update a room", description = "Changes the number, floor, type, capacity and price of a room")
    public ResponseEntity<?> updateRoom(@PathVariable("hotelId") Long hotelId, @PathVariable("roomId") Long roomId,
                                        @Valid @RequestBody CreateRoomRequest request) {
        return roomService.updateRoom(hotelId, roomId, request);
    }

    @PatchMapping("/{roomId}/status")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Change room status", description = "ACTIVE, INACTIVE or UNDER_MAINTENANCE. A room with upcoming bookings can't be switched off")
    public ResponseEntity<?> updateRoomStatus(@PathVariable("hotelId") Long hotelId, @PathVariable("roomId") Long roomId,
                                              @Valid @RequestBody UpdateRoomStatusRequest request) {
        return roomService.updateRoomStatus(hotelId, roomId, request);
    }
}
