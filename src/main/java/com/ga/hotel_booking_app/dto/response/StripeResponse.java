package com.ga.hotel_booking_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StripeResponse {
//    private String status;
//    private String message;
//    private String sessionId;
    private String sessionURL;
}
