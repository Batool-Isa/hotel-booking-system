package com.ga.hotelservice.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthUser {
    private final Long id;
    private final String email;
    private final String role;
    private final String status;

    public boolean isAdmin()   { return "ADMIN".equals(role); }
    public boolean isManager() { return "HOTEL_MANAGER".equals(role); }
}