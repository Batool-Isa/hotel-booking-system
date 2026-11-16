package com.ga.hotelservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HotelStatsResponse {
    private long hotelsTotal;
    private Map<String, Long> hotelsByStatus;
}
