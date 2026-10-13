package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.response.AdminUserResponse;
import com.ga.hotel_booking_app.dto.response.MessageResponse;
import com.ga.hotel_booking_app.dto.response.PageResponse;
import com.ga.hotel_booking_app.dto.request.UpdateUserStatusRequest;
import com.ga.hotel_booking_app.model.Role;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin only controller with endpoints to manage users app
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Users", description = "List all users app and to activate/deactivate user")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Not logged in"),
        @ApiResponse(responseCode = "403", description = "Forbidden : Admin role required")
})
public class AdminUserController {
    @Autowired
    private AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "List users", description = "Fetches all users with ability to filter, sort and search by name , role etc. ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Get page of users"),
            @ApiResponse(responseCode = "400", description = "Invalid filter option")
    })
    public ResponseEntity<PageResponse<AdminUserResponse>> getUsers(
          @RequestParam(required = false) User.Status status, @RequestParam(required = false) Role.RoleName role, @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return adminUserService.getUsers(status, role, search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Get user"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<AdminUserResponse> getUser(@PathVariable Long id) {

        return adminUserService.getUser(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a user",
            description = "Change user state to active or inactive")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Update user state"),
            @ApiResponse(responseCode = "400", description = "Bad request :Invalid information"),
            @ApiResponse(responseCode = "404", description = "User not found")

    })
    public ResponseEntity<MessageResponse> updateStatus(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateUserStatusRequest request) {
        return adminUserService.updateStatus(id, request.getStatus());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user",
            description = "Soft delete for the user by changing his status to inactive")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Update user state to inactive"),
            @ApiResponse(responseCode = "400", description = "Bad request :Invalid information"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        return adminUserService.deleteUser(id);

    }
}
