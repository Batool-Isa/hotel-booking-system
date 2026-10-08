package com.ga.hotel_booking_app.dto.reponse;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminUserResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String profileImageUrl;
    private String role;
    private String status;
    private boolean emailVerified;
    private LocalDateTime createdAt;
}
