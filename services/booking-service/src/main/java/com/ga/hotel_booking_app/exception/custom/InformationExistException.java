package com.ga.hotel_booking_app.exception.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 409 Error thrown when user try to create information is already exists.
 * like when user try to register with email that already used.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class InformationExistException extends RuntimeException {
    public InformationExistException(String message) {
        super(message);
    }

}
