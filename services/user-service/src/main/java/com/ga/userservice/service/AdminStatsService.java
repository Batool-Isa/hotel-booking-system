package com.ga.userservice.service;

import com.ga.userservice.client.BookingStats;
import com.ga.userservice.client.HotelStats;
import com.ga.userservice.client.StatsClient;
import com.ga.userservice.dto.response.AdminStatsResponse;
import com.ga.userservice.model.Role;
import com.ga.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminStatsService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StatsClient statsClient;

    public ResponseEntity<?> getStats() {
        BookingStats bookingStats = statsClient.getBookingStats();
        HotelStats hotelStats = statsClient.getHotelStats();

        AdminStatsResponse r = new AdminStatsResponse();

        r.setRevenueTotal(bookingStats.getRevenueTotal());
        r.setRevenueThisMonth(bookingStats.getRevenueThisMonth());
        r.setRevenueLastMonth(bookingStats.getRevenueLastMonth());
        r.setAverageBookingValue(bookingStats.getAverageBookingValue());
        r.setBookingsTotal(bookingStats.getBookingsTotal());
        r.setBookingsThisMonth(bookingStats.getBookingsThisMonth());
        r.setBookingsLastMonth(bookingStats.getBookingsLastMonth());
        r.setCancellationRate(bookingStats.getCancellationRate());
        r.setBookingsByStatus(bookingStats.getBookingsByStatus());
        r.setReviewsTotal(bookingStats.getReviewsTotal());
        r.setAverageRating(bookingStats.getAverageRating());
        r.setDaily(bookingStats.getDaily());
        r.setTopHotels(bookingStats.getTopHotels());

        r.setHotelsTotal(hotelStats.getHotelsTotal());
        r.setHotelsByStatus(hotelStats.getHotelsByStatus());

        LocalDateTime thisMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Map<String, Long> usersByRole = new LinkedHashMap<>();
        for (Role.RoleName role : Role.RoleName.values()) {
            usersByRole.put(role.name(), 0L);
        }
        for (Object[] row : userRepository.countGroupByRole()) {
            if (row[0] != null) {
                usersByRole.put(((Role.RoleName) row[0]).name(), (Long) row[1]);
            }
        }
        r.setUsersByRole(usersByRole);
        r.setUsersTotal(userRepository.count());
        r.setUsersNewThisMonth(userRepository.countByCreatedAtGreaterThanEqual(thisMonth));

        return ResponseEntity.ok(r);
    }
}
