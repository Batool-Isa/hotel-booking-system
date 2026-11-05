package com.ga.hotel_booking_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AdminStatsResponse {
    private BigDecimal revenueTotal;
    private BigDecimal revenueThisMonth;
    private BigDecimal revenueLastMonth;
    private BigDecimal averageBookingValue;
    private long bookingsTotal;
    private long bookingsThisMonth;
    private long bookingsLastMonth;
    private double cancellationRate;
    private Map<String, Long> bookingsByStatus;
    private long hotelsTotal;
    private Map<String, Long> hotelsByStatus;
    private long usersTotal;
    private long usersNewThisMonth;
    private Map<String, Long> usersByRole;
    private long reviewsTotal;
    private double averageRating;
    private List<DailyStat> daily;
    private List<TopHotel> topHotels;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyStat {
        private LocalDate date;
        private long bookings;
        private BigDecimal revenue;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopHotel {
        private Long hotelId;
        private String name;
        private long bookings;
        private BigDecimal revenue;
    }
}
