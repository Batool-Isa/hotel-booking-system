package com.ga.hotel_booking_app.dto.reponse;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonPropertyOrder({"infantMaxAge","childMaxAge","updatedAt"})
public class ChildPolicyResponse {
    private Integer infantMaxAge;
    private Integer childMaxAge;
    private LocalDateTime updatedAt;

}