package com.ga.hotel_booking_app.dto.request;

import lombok.Getter;

@Getter
public class ResetPasswordRequest {
    private String password;
    private String confirmedPassword;

}
