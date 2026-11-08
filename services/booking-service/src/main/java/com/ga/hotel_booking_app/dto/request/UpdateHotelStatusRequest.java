package com.ga.hotel_booking_app.dto.request;

import com.ga.hotel_booking_app.model.Hotel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class UpdateHotelStatusRequest {

    @NotNull
    private Hotel.Status status;


}
