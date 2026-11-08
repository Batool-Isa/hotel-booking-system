package com.ga.hotel_booking_app.dto.request;

import com.ga.hotel_booking_app.model.RoomType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateRoomTypeStatusRequest {

    @NotNull
    private RoomType.Status status;


}
