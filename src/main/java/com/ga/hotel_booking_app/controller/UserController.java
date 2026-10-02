package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.*;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.service.PasswordResetService;
import com.ga.hotel_booking_app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/auth/users")
@Tag(name = "User Management", description = "APIs for managing user accounts")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping(
            value = "/register",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Register a new user", description = "Create a new user account and send a verification email")
    public ResponseEntity<?> register(@Valid @ModelAttribute RegisterRequest request,
                                      @RequestParam("image") MultipartFile image) {
        System.out.println("User Controller calling ---> register");
        return userService.register(request, image);
    }

    @PostMapping("/login")
    @Operation(summary = "login", description = "Check user credentials and generate a JWT token")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        System.out.println("User Controller calling ---> login");
        return userService.loginUser(loginRequest);
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email to activate user account", description = "Check if token is valid and update user status")
    public String verifyEmail(@RequestParam(name = "token") String token) {
        System.out.println("User Controller calling ---> verfiy email");
        return userService.verifyEmail(token);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forget Password", description = "Send email with reset password link")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        System.out.println("User Controller calling ---> forget password");
        return passwordResetService.forgotPassword(request);
    }

    @PostMapping("/reset-link")
    @Operation(summary = "Reset Password", description = "Check reset token validity and update password")
    public ResponseEntity<?> resetPassword(@RequestParam String token,
                                          @Valid @RequestBody ResetPasswordRequest request) throws BadRequestException {
        System.out.println("User Controller calling ---> reset password");
        return passwordResetService.resetPassword(token, request);
    }
    @PostMapping("/change-password")
    @Operation(summary = "Change Password", description = "Change user password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request)  {
        System.out.println("User Controller calling ---> change password");
        return passwordResetService.changePassword(request);
    }
    @GetMapping("/profile")
    @Operation(summary = "Get User Profile", description = "Fetches all user details")
    public ResponseEntity<?> getUserProfile()  {
        System.out.println("User Controller calling ---> get user profile");
        return userService.getUserProfile();
    }
    @PutMapping("/profile")
    @Operation(summary = "Update User Profile", description = "Updates all user profiles")
    public ResponseEntity<?> updateUserProfile(@Valid @RequestBody UpdateProfileRequest request)  {
        System.out.println("User Controller calling ---> update user profile");
        return userService.updateUserProfile(request);
    }
    @PutMapping(
            value = "/profile/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Update Image Profile", description = "Updates image profile")
    public ResponseEntity<?> updateProfileImage(
            @RequestParam("image") MultipartFile image) {
        System.out.println("User Controller calling ---> update user profile image");

        return userService.updateProfileImage(image);
    }
}
