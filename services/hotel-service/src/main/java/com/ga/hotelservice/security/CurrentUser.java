package com.ga.hotelservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class CurrentUser {
    public AuthUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null) {
            return  null;
        }
        return auth.getPrincipal() instanceof AuthUser user ? user : null;
    }

    public String token() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        return request.getHeader(HttpHeaders.AUTHORIZATION);
    }
}
