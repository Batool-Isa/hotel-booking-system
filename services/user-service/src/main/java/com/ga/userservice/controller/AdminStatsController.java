//package com.ga.userservice.controller;
//
////import com.ga.bookingservice.dto.response.AdminStatsResponse;
////import com.ga.bookingservice.service.AdminStatsService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/admin/stats")
//@PreAuthorize("hasRole('ADMIN')")
//@Tag(name = "Admin - Statistics", description = "Dashboard statistics for the admin")
//public class AdminStatsController {
//    private final AdminStatsService adminStatsService;
//
//    public AdminStatsController(AdminStatsService adminStatsService) {
//        this.adminStatsService = adminStatsService;
//    }
//
//    @GetMapping
//    @Operation(summary = "Get admin statistics", description = "Revenue, bookings, hotels, users, reviews .. etc")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Statistics"),
//            @ApiResponse(responseCode = "401", description = "Not logged in"),
//            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
//    })
//    public ResponseEntity<?> getStats() {
//        return adminStatsService.getStats();
//    }
//}
