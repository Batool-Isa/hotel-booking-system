package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.CreateHotelRequest;
import com.ga.hotel_booking_app.dto.request.RegisterRequest;
import com.ga.hotel_booking_app.service.HotelService;
import com.ga.hotel_booking_app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hotels")
public class HotelController {

    @Autowired
    private HotelService hotelService;
    @Autowired
    private UserService userService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_MANAGER')")
    @Operation(summary = "Create a new hotel", description = "Create a new hotel account and send a verification email")
    public ResponseEntity<?> createHotel(@Valid @RequestBody CreateHotelRequest request) {
        System.out.println("Hotel Controller calling ---> create");
        return hotelService.create(request);
    }

}
