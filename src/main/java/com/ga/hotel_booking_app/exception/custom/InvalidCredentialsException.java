package com.ga.hotel_booking_app.exception.custom;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 401 Error thrown when user try to login using invalid credentials
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCredentialsException extends BadCredentialsException {
    public InvalidCredentialsException(@Nullable String msg) {
        super(msg);
    }
}
