package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.CreateRoomRequest;
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

//    POST   /api/hotels/{hotelId}/rooms
//    GET    /api/hotels/{hotelId}/rooms
//    GET    /api/hotels/{hotelId}/rooms/{roomId}
//    PUT    /api/hotels/{hotelId}/rooms/{roomId}
//    PATCH  /api/hotels/{hotelId}/rooms/{roomId}/status
//Create multiple rooms
//    POST /api/hotels/{hotelId}/rooms/bulk
//
//
//    Import from Excel
//    POST /api/hotels/{hotelId}/rooms/import
@Autowired
private RoomService roomService;
    @PostMapping
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Create new room", description = "Create a new room in a hotel")
    public ResponseEntity<?> createRoom(@PathVariable("hotelId") Long id, @Valid @RequestBody CreateRoomRequest request){
        System.out.println("Room controller calling ---> create room");
        return roomService.createRoom(id, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Get hotels room", description = "Fetches all hotel room")
    public ResponseEntity<?> getHotelRooms(@PathVariable("hotelId") Long id, Pageable pageable){
        System.out.println("Room controller calling ---> get hotel rooms");
        return roomService.getHotelRooms(id, pageable);
    }
//    @GetMapping
//    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
//    @Operation(summary = "Get Room Types", description = "Fetches all room types details")
//    public ResponseEntity<?> getRoomTypes(){
//        System.out.println("Room Types controller calling ---> get room types");
//        return roomTypeService.getRoomTypes();
//    }
//    @GetMapping("{id}")
//    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
//    @Operation(summary = "Get Room Type", description = "Fetches a room types details")
//    public ResponseEntity<?> getRoomType(@PathVariable("id") Long id){
//        System.out.println("Room Types controller calling ---> get room type type");
//        return roomTypeService.getRoomType(id);
//    }
//
//    @PutMapping("{id}")
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(summary = "Update a room type", description = "Update a new room type details")
//    public ResponseEntity<?> updateRoomType(@PathVariable("id") Long id,@Valid @RequestBody RoomTypeRequest request){
//        System.out.println("Room Types controller calling ---> update room type");
//        return roomTypeService.updateRoomType(id, request);
//    }
//    @PatchMapping("{id}/status")
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(summary = "Update a room type status", description = "Update a new room type status")
//    public ResponseEntity<?> updateRoomTypeStatus(@PathVariable("id") Long id,@Valid @RequestBody UpdateRoomTypeStatusRequest request){
//        System.out.println("Room Types controller calling ---> update room type status");
//        return roomTypeService.updateRoomTypeStatus(id, request);
//    }
}
