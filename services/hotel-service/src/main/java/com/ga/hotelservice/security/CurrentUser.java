package com.ga.hotelservice.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    public AuthUser get() {
        return (AuthUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
