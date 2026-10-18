package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.*;
import com.ga.hotel_booking_app.service.PasswordResetService;
import com.ga.hotel_booking_app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
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
    @Operation(summary = "Register a new user", description = "Creates a new customer account and sends an email to verify it. A profile image is optional")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created, verification email sent"),
            @ApiResponse(responseCode = "400", description = "Invalid information, passwords do not match, or invalid image"),
            @ApiResponse(responseCode = "409", description = "Email or username already exists")
    })
    public ResponseEntity<?> register(@Valid @ModelAttribute RegisterRequest request,
                                      @RequestParam(value = "image", required = false) MultipartFile image) {
        System.out.println("User Controller calling ---> register");
        return userService.register(request, image);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Checks your email and password and gives you a JWT token to use in other requests")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged in, JWT token returned"),
            @ApiResponse(responseCode = "400", description = "Email or password is missing"),
            @ApiResponse(responseCode = "401", description = "Wrong email or password, email not verified, or account inactive")
    })
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        System.out.println("User Controller calling ---> login");
        return userService.loginUser(loginRequest);
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out", description = "Logs you out and saves it in the audit log")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @ApiResponse(responseCode = "401", description = "Not logged in")
    })
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        System.out.println("User Controller calling ---> logout");
        return userService.logoutUser(request);
    }
    @GetMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Opens from the link in the verification email and activates your account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified, account is now active"),
            @ApiResponse(responseCode = "400", description = "Token has expired or was already used"),
            @ApiResponse(responseCode = "404", description = "Token not found")
    })
    public String verifyEmail(@RequestParam(name = "token") String token) {
        System.out.println("User Controller calling ---> verfiy email");
        return userService.verifyEmail(token);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Sends a reset password link to your email if an account exists with it")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "If the email exists, a reset link has been sent"),
            @ApiResponse(responseCode = "400", description = "Invalid email")
    })
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        System.out.println("User Controller calling ---> forget password");
        return passwordResetService.forgotPassword(request);
    }

    @PostMapping("/reset-link")
    @Operation(summary = "Reset password", description = "Uses the token from the reset email to set a new password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Token expired or already used, or passwords do not match"),
            @ApiResponse(responseCode = "404", description = "Reset token not found")
    })
    public ResponseEntity<?> resetPassword(@RequestParam String token,
                                          @Valid @RequestBody ResetPasswordRequest request) throws BadRequestException {
        System.out.println("User Controller calling ---> reset password");
        return passwordResetService.resetPassword(token, request);
    }
    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Changes your password. You need to enter your current password first")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "New password and confirmation do not match"),
            @ApiResponse(responseCode = "401", description = "Not logged in or current password is wrong")
    })
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request)  {
        System.out.println("User Controller calling ---> change password");
        return passwordResetService.changePassword(request);
    }
    @GetMapping("/profile")
    @Operation(summary = "Get my profile", description = "Shows the details of the logged in user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile details"),
            @ApiResponse(responseCode = "401", description = "Not logged in")
    })
    public ResponseEntity<?> getUserProfile()  {
        System.out.println("User Controller calling ---> get user profile");
        return userService.getUserProfile();
    }
    @PutMapping("/profile")
    @Operation(summary = "Update my profile", description = "Changes the details of the logged in user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid profile information"),
            @ApiResponse(responseCode = "401", description = "Not logged in")
    })
    public ResponseEntity<?> updateUserProfile(@Valid @RequestBody UpdateProfileRequest request)  {
        System.out.println("User Controller calling ---> update user profile");
        return userService.updateUserProfile(request);
    }
    @PutMapping(
            value = "/profile/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Update my profile image", description = "Replaces your profile picture. Only JPG, PNG or WebP up to 1 MB")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile image updated successfully"),
            @ApiResponse(responseCode = "400", description = "No image chosen, image too large, or wrong file type"),
            @ApiResponse(responseCode = "401", description = "Not logged in")
    })
    public ResponseEntity<?> updateProfileImage(
            @RequestParam("image") MultipartFile image) {
        System.out.println("User Controller calling ---> update user profile image");
        return userService.updateProfileImage(image);
    }
}
