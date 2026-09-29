package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
