package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.*;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.service.PasswordResetService;
import com.ga.hotel_booking_app.service.UserService;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/users")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        System.out.println("User Controller calling ---> register");
        return userService.register(request);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        System.out.println("User Controller calling ---> login");
        return userService.loginUser(loginRequest);
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam(name = "token") String token) {
        System.out.println("User Controller calling ---> verfiy email");
        return userService.verifyEmail(token);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        System.out.println("User Controller calling ---> forget password");
        return passwordResetService.forgotPassword(request);
    }

    @PostMapping("/reset-link")
    public ResponseEntity<?> resetPassword(@RequestParam String token,
                                          @Valid @RequestBody ResetPasswordRequest request) throws BadRequestException {
        System.out.println("User Controller calling ---> reset password");
        return passwordResetService.resetPassword(token, request);
    }
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request)  {
        System.out.println("User Controller calling ---> change password");
        return passwordResetService.changePassword(request);
    }
}
