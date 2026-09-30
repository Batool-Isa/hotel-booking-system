package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.dto.request.LoginRequest;
import com.ga.hotel_booking_app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public User register(@RequestBody User user){
        System.out.println("User Controller calling ---> register");
    return userService.register(user);
    }
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest){
        System.out.println("User Controller calling ---> login");
        return userService.loginUser(loginRequest);
    }
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam(name = "token") String token){
        System.out.println("User Controller calling ---> verfiy email");
        return userService.verifyEmail(token);
    }
}
