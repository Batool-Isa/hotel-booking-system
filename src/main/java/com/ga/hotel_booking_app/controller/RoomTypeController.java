package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.RoomTypeRequest;
import com.ga.hotel_booking_app.dto.request.UpdateRoomTypeStatusRequest;
import com.ga.hotel_booking_app.service.RoomTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms-types")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Not logged in")
})
public class RoomTypeController {

@Autowired
private RoomTypeService roomTypeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Get all room types", description = "Shows every room type (like Single, Double or Suite) that rooms can use")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of room types"),
            @ApiResponse(responseCode = "403", description = "Only admin or hotel manager can view room types")
    })
    public ResponseEntity<?> getRoomTypes(){
        System.out.println("Room Types controller calling ---> get room types");
        return roomTypeService.getRoomTypes();
    }
    @GetMapping("{id}")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER','ADMIN')")
    @Operation(summary = "Get one room type", description = "Shows the details of one room type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room type details"),
            @ApiResponse(responseCode = "403", description = "Only admin or hotel manager can view room types"),
            @ApiResponse(responseCode = "404", description = "Room type not found")
    })
    public ResponseEntity<?> getRoomType(@PathVariable("id") Long id){
        System.out.println("Room Types controller calling ---> get room type type");
        return roomTypeService.getRoomType(id);
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a room type", description = "Adds a new room type that hotels can use for their rooms")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Room type created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid room type information"),
            @ApiResponse(responseCode = "403", description = "Only admin can create a room type"),
            @ApiResponse(responseCode = "409", description = "Room type with the same name already exists")
    })
    public ResponseEntity<?> createRoomType(@Valid @RequestBody RoomTypeRequest request){
        System.out.println("Room Types controller calling ---> create room type");
        return roomTypeService.createRoomType(request);
    }
    @PutMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a room type", description = "Changes the name or details of a room type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room type updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid room type information"),
            @ApiResponse(responseCode = "403", description = "Only admin can update a room type"),
            @ApiResponse(responseCode = "404", description = "Room type not found"),
            @ApiResponse(responseCode = "409", description = "Another room type already has this name")
    })
    public ResponseEntity<?> updateRoomType(@PathVariable("id") Long id,@Valid @RequestBody RoomTypeRequest request){
        System.out.println("Room Types controller calling ---> update room type");
        return roomTypeService.updateRoomType(id, request);
    }
    @PatchMapping("{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change room type status", description = "Activates or deactivates a room type. Inactive room types can't be used for new rooms")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room type status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "403", description = "Only admin can change a room type status"),
            @ApiResponse(responseCode = "404", description = "Room type not found")
    })
    public ResponseEntity<?> updateRoomTypeStatus(@PathVariable("id") Long id,@Valid @RequestBody UpdateRoomTypeStatusRequest request){
        System.out.println("Room Types controller calling ---> update room type status");
        return roomTypeService.updateRoomTypeStatus(id, request);
    }
}
