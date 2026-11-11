//package com.ga.userservice.service;
//
//import com.ga.userservice.dto.response.AdminStatsResponse;
//import com.ga.userservice.model.Booking;
//import com.ga.userservice.model.Hotel;
//import com.ga.userservice.model.Role;
//import com.ga.userservice.repository.BookingRepository;
//import com.ga.userservice.repository.HotelRepository;
//import com.ga.userservice.repository.ReviewRepository;
//import com.ga.userservice.repository.UserRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.*;
//
//@Service
//public class AdminStatsService {
//    private static final List<Booking.Status> PAID = List.of(Booking.Status.CONFIRMED, Booking.Status.COMPLETED);
//    private static final int DAYS = 30;
//    @Autowired
//    private BookingRepository bookingRepository;
//    @Autowired
//    private HotelRepository hotelRepository;
//    @Autowired
//    private UserRepository userRepository;
//    @Autowired
//    private ReviewRepository reviewRepository;
//
//
//    @Transactional(readOnly = true)
//    public ResponseEntity<?> getStats() {
//        LocalDate today = LocalDate.now();
//        LocalDateTime thisMonth = today.withDayOfMonth(1).atStartOfDay();
//        LocalDateTime nextMonth = thisMonth.plusMonths(1);
//        LocalDateTime lastMonth = thisMonth.minusMonths(1);
//        // compare like with like: the 1st..now of this month against the same days of last month
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime lastMonthSamePoint = lastMonth.plus(java.time.Duration.between(thisMonth, now));
//        if (lastMonthSamePoint.isAfter(thisMonth)) {
//            lastMonthSamePoint = thisMonth;
//        }
//        AdminStatsResponse r = new AdminStatsResponse();
//        Map<String, Long> bookingsByStatus = new LinkedHashMap<>();
//        for (Booking.Status s : Booking.Status.values())
//            bookingsByStatus.put(s.name(), 0L);
//        for (Object[] row : bookingRepository.countGroupByStatus())
//            bookingsByStatus.put(((Booking.Status) row[0]).name(), (Long) row[1]);
//        long bookingsTotal = bookingsByStatus.values().stream().mapToLong(Long::longValue).sum();
//        long paidCount = bookingsByStatus.get(Booking.Status.CONFIRMED.name()) + bookingsByStatus.get(Booking.Status.COMPLETED.name());
//        BigDecimal revenueTotal = bookingRepository.sumRevenue(PAID);
//        r.setBookingsByStatus(bookingsByStatus);
//        r.setBookingsTotal(bookingsTotal);
//        r.setBookingsThisMonth(bookingRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(thisMonth, nextMonth));
//        r.setBookingsLastMonth(bookingRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(lastMonth, lastMonthSamePoint));
//        r.setCancellationRate(bookingsTotal == 0 ? 0 : Math.round(bookingsByStatus.get(Booking.Status.CANCELLED.name()) * 1000.0 / bookingsTotal) / 10.0);
//        r.setRevenueTotal(revenueTotal);
//        r.setRevenueThisMonth(bookingRepository.sumRevenueBetween(PAID, thisMonth, nextMonth));
//        r.setRevenueLastMonth(bookingRepository.sumRevenueBetween(PAID, lastMonth, lastMonthSamePoint));
//        r.setAverageBookingValue(paidCount == 0 ? BigDecimal.ZERO : revenueTotal.divide(BigDecimal.valueOf(paidCount), 2, RoundingMode.HALF_UP));
//        Map<String, Long> hotelsByStatus = new LinkedHashMap<>();
//        for (Hotel.Status s : Hotel.Status.values())
//            hotelsByStatus.put(s.name(), 0L);
//        for (Object[] row : hotelRepository.countGroupByStatus())
//            hotelsByStatus.put(((Hotel.Status) row[0]).name(), (Long) row[1]);
//        r.setHotelsByStatus(hotelsByStatus);
//        r.setHotelsTotal(hotelsByStatus.values().stream().mapToLong(Long::longValue).sum());
//        Map<String, Long> usersByRole = new LinkedHashMap<>();
//        for (Role.RoleName role : Role.RoleName.values())
//            usersByRole.put(role.name(), 0L);
//        for (Object[] row : userRepository.countGroupByRole())
//            if (row[0] != null) usersByRole.put(((Role.RoleName) row[0]).name(), (Long) row[1]);
//        r.setUsersByRole(usersByRole);
//        r.setUsersTotal(userRepository.count());
//        r.setUsersNewThisMonth(userRepository.countByCreatedAtGreaterThanEqual(thisMonth));
//        Double avg = reviewRepository.averageRating();
//        r.setReviewsTotal(reviewRepository.count());
//        r.setAverageRating(avg == null ? 0 : Math.round(avg * 10) / 10.0);
//        LocalDate from = today.minusDays(DAYS - 1);
//        Map<LocalDate, Object[]> byDay = new HashMap<>();
//        for (Object[] row : bookingRepository.dailyTotalsSince(PAID, from.atStartOfDay()))
//            byDay.put((LocalDate) row[0], row);
//        List<AdminStatsResponse.DailyStat> daily = new ArrayList<>();
//        for (int i = 0; i < DAYS; i++) {
//            LocalDate d = from.plusDays(i);
//            Object[] row = byDay.get(d);
//            daily.add(row == null ? new AdminStatsResponse.DailyStat(d, 0, BigDecimal.ZERO) : new AdminStatsResponse.DailyStat(d, (Long) row[1], (BigDecimal) row[2]));
//        }
//        r.setDaily(daily);
//        List<AdminStatsResponse.TopHotel> top = new ArrayList<>();
//        for (Object[] row : bookingRepository.topHotelsByRevenue(PAID, PageRequest.of(0, 5)))
//            top.add(new AdminStatsResponse.TopHotel((Long) row[0], (String) row[1], (Long) row[2], (BigDecimal) row[3]));
//        r.setTopHotels(top);
//        return ResponseEntity.ok(r);
//    }
//}
