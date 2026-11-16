package com.ga.userservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ga.userservice.dto.response.AdminStatsResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingStats {
    private BigDecimal revenueTotal;
    private BigDecimal revenueThisMonth;
    private BigDecimal revenueLastMonth;
    private BigDecimal averageBookingValue;
    private long bookingsTotal;
    private long bookingsThisMonth;
    private long bookingsLastMonth;
    private double cancellationRate;
    private Map<String, Long> bookingsByStatus;
    private long reviewsTotal;
    private double averageRating;
    private List<AdminStatsResponse.DailyStat> daily;
    private List<AdminStatsResponse.TopHotel> topHotels;
}
