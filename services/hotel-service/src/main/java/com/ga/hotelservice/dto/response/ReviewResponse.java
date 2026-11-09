package com.ga.hotelservice.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReviewResponse {
    private Long id;
    private double rating;
    private String comment;
    private String guestName;
    private LocalDateTime createdAt;
}
