package com.ga.hotelservice.dto.request;

import com.ga.hotelservice.model.Hotel;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateHotelStatusRequest {

    @NotNull
    private Hotel.Status status;


}
