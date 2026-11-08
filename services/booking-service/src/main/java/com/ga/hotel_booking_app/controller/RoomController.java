package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.BulkCreateRoomsRequest;
import com.ga.hotel_booking_app.dto.request.CreateRoomRequest;
import com.ga.hotel_booking_app.dto.request.UpdateRoomStatusRequest;
import com.ga.hotel_booking_app.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Create room successfully"),
            @ApiResponse(responseCode = "404", description = "Hotel or room type not found"),
            @ApiResponse(responseCode = "400", description = "Invalid hotel status"),
            @ApiResponse(responseCode = "403", description = "Only admin or assigned manager can create a hotel's room"),
            @ApiResponse(responseCode = "409", description = "Room number already exists in this hotel"),
    })
    public ResponseEntity<?> createRoom(@PathVariable("hotelId") Long id,
            @Valid @RequestBody CreateRoomRequest request) {
        System.out.println("Room controller calling ---> create room");
        return roomService.createRoom(id, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Get hotels room", description = "Fetches all hotel room")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List hotel's rooms"),
            @ApiResponse(responseCode = "404", description = "Hotel or manager are not found"),
            @ApiResponse(responseCode = "400", description = "Invalid hotel status"),
            @ApiResponse(responseCode = "403", description = "Only admin or assigned manager can view all hotel's rooms includeing under maintance and inactive .. etc"),
    })
    public ResponseEntity<?> getHotelRooms(@PathVariable("hotelId") Long id, Pageable pageable) {
        System.out.println("Room controller calling ---> get hotel rooms");
        return roomService.getHotelRooms(id, pageable);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Create many rooms", description = "Adds up to 100 rooms in one request. If any room is invalid, none of them are saved")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "All rooms created successfully"),
            @ApiResponse(responseCode = "400", description = "Hotel is not active, a room number is repeated in the list, or a room type is not active"),
            @ApiResponse(responseCode = "403", description = "Only admin or assigned manager can add rooms to this hotel"),
            @ApiResponse(responseCode = "404", description = "Hotel or room type not found"),
            @ApiResponse(responseCode = "409", description = "A room number already exists in this hotel")
    })
    public ResponseEntity<?> createRoomsBulk(@PathVariable("hotelId") Long id,
            @Valid @RequestBody BulkCreateRoomsRequest request) {
        return roomService.createRoomsBulk(id, request);
    }

    @GetMapping("/{roomId}")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Get one room", description = "Shows the details of one room in this hotel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room details"),
            @ApiResponse(responseCode = "403", description = "Manager can only view rooms of their own hotels"),
            @ApiResponse(responseCode = "404", description = "Hotel not found or room not found in this hotel")
    })
    public ResponseEntity<?> getRoom(@PathVariable("hotelId") Long hotelId, @PathVariable("roomId") Long roomId) {
        return roomService.getRoom(hotelId, roomId);
    }

    @PutMapping("/{roomId}")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Update a room", description = "Changes the room number, floor, type, capacity or price of a room")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid room information or room type is not active"),
            @ApiResponse(responseCode = "403", description = "Manager can only update rooms of their own hotels"),
            @ApiResponse(responseCode = "404", description = "Hotel, room or room type not found"),
            @ApiResponse(responseCode = "409", description = "Room number already exists in this hotel")
    })
    public ResponseEntity<?> updateRoom(@PathVariable("hotelId") Long hotelId, @PathVariable("roomId") Long roomId,
            @Valid @RequestBody CreateRoomRequest request) {
        return roomService.updateRoom(hotelId, roomId, request);
    }

    @PatchMapping("/{roomId}/status")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Change room status", description = "Sets the room to ACTIVE, INACTIVE or UNDER_MAINTENANCE. A room with upcoming bookings can't be turned off")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Room already has this status or has upcoming bookings"),
            @ApiResponse(responseCode = "403", description = "Manager can only update rooms of their own hotels"),
            @ApiResponse(responseCode = "404", description = "Hotel not found or room not found in this hotel")
    })
    public ResponseEntity<?> updateRoomStatus(@PathVariable("hotelId") Long hotelId,
            @PathVariable("roomId") Long roomId,
            @Valid @RequestBody UpdateRoomStatusRequest request) {
        return roomService.updateRoomStatus(hotelId, roomId, request);
    }
}
