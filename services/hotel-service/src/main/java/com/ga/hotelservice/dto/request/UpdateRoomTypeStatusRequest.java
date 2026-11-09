package com.ga.hotelservice.dto.request;

import com.ga.hotelservice.model.RoomType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateRoomTypeStatusRequest {

    @NotNull
    private RoomType.Status status;


}
