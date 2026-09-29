package com.ga.hotel_booking_app.exception.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 404 Error thrown when user try to retrieve an information that does not exist
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class InformationNotFoundException extends RuntimeException {
    public InformationNotFoundException(String message) {
        super(message);
    }

}
