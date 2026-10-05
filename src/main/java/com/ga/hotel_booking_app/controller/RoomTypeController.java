package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.RoomTypeRequest;
import com.ga.hotel_booking_app.dto.request.UpdateRoomTypeStatusRequest;
import com.ga.hotel_booking_app.service.RoomTypeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms-types")
public class RoomTypeController {

//    POST   /api/room-types
//    GET    /api/room-types
//    GET    /api/room-types/{id}
//    PUT    /api/room-types/{id}
    @Autowired
    private RoomTypeService roomTypeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Get Room Types", description = "Fetches all room types details")
    public ResponseEntity<?> getRoomTypes(){
        System.out.println("Room Types controller calling ---> get room types");
        return roomTypeService.getRoomTypes();
    }
    @GetMapping("{id}")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Get Room Type", description = "Fetches a room types details")
    public ResponseEntity<?> getRoomType(@PathVariable("id") Long id){
        System.out.println("Room Types controller calling ---> get room type type");
        return roomTypeService.getRoomType(id);
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new room type", description = "Create a new room type")
    public ResponseEntity<?> createRoomType(@Valid @RequestBody RoomTypeRequest request){
        System.out.println("Room Types controller calling ---> create room type");
        return roomTypeService.createRoomType(request);
    }
    @PutMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a room type", description = "Update a new room type details")
    public ResponseEntity<?> updateRoomType(@PathVariable("id") Long id,@Valid @RequestBody RoomTypeRequest request){
        System.out.println("Room Types controller calling ---> update room type");
        return roomTypeService.updateRoomType(id, request);
    }
    @PatchMapping("{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a room type status", description = "Update a new room type status")
    public ResponseEntity<?> updateRoomTypeStatus(@PathVariable("id") Long id,@Valid @RequestBody UpdateRoomTypeStatusRequest request){
        System.out.println("Room Types controller calling ---> update room type status");
        return roomTypeService.updateRoomTypeStatus(id, request);
    }
}
