package com.ga.hotel_booking_app.exception.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *  409 error is thrown when user try to book a room not available at that time
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class RoomUnavailableException extends RuntimeException {
    public RoomUnavailableException(String message) {
        super(message);
    }
}